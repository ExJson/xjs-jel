package xjs.jel.serialization.sequence;

import org.junit.jupiter.api.Test;
import xjs.jel.destructuring.ArrayDestructurePattern;
import xjs.jel.destructuring.DestructurePattern;
import xjs.jel.destructuring.KeyPattern;
import xjs.jel.destructuring.ObjectDestructurePattern;
import xjs.jel.exception.JelException;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.Tokenizer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DestructureParserTest {

    private static final DestructureParser PARSER = Sequencer.JEL.destructureParser;

    @Test
    public void parse_readsSimpleObjectPattern() throws JelException {
        final DestructurePattern pattern = parse("{ a }");

        final ObjectDestructurePattern object =
            assertInstanceOf(ObjectDestructurePattern.class, pattern);
        final List<KeyPattern> keys = object.keys;

        assertEquals(1, keys.size());
        assertEquals("a", keys.get(0).key);
    }

    @Test
    public void parseObject_readsRenameToken() throws JelException {
        final DestructurePattern pattern = parse("{ key: rename }");

        final List<KeyPattern> keys = ((ObjectDestructurePattern) pattern).keys;
        assertEquals(1, keys.size());
        assertEquals("key", keys.get(0).key);
        assertEquals("rename", keys.get(0).source);
    }

    @Test
    public void parseObject_readsMultipleKeys() throws JelException {
        final DestructurePattern pattern = parse("{ a, b: x, c }");

        final List<KeyPattern> keys = ((ObjectDestructurePattern) pattern).keys;
        assertEquals(3, keys.size());
        assertEquals("a", keys.get(0).key);
        assertEquals("b", keys.get(1).key);
        assertEquals("x", keys.get(1).source);
        assertEquals("c", keys.get(2).key);
    }

    @Test
    public void parseObject_toleratesWhitespaceAndComments() throws JelException {
        final DestructurePattern pattern = parse("{ \n a \n b // comment\n c }");

        final List<KeyPattern> keys = ((ObjectDestructurePattern) pattern).keys;
        assertEquals(3, keys.size());
        assertEquals("a", keys.get(0).key);
        assertEquals("b", keys.get(1).key);
        assertEquals("c", keys.get(2).key);
    }

    @Test
    public void parseObject_doesNotTolerate_extraneousTokens() {
        final String text = "{ a , % b }";
        final String message = """
            JelException: Illegal identifier
            ---------------------------------------------------
                0 | { a , % b }
                          ^
            ---------------------------------------------------
            Hint: should match the following syntax: { k: r }""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parseObject_doesNotTolerate_missingRename() {
        final String text = "{ key: }";
        final String message = """
            JelException: Expected source identifier
            ---------------------------------------------------
                0 | { key: }
                         ^
            ---------------------------------------------------
            Hint: should match the following syntax: { k: r }""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parseObject_doesNotTolerate_illegalRename() {
        final String text = "{ key: 1234 }";
        final String message = """
            JelException: Illegal source identifier
            ---------------------------------------------------
                0 | { key: 1234 }
                           ^^^^
            ---------------------------------------------------
            Hint: should match the following syntax: { k: r }""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parse_readsSimpleArrayPattern() throws JelException {
        final DestructurePattern pattern = parse("[ a ]");

        final ArrayDestructurePattern array =
            assertInstanceOf(ArrayDestructurePattern.class, pattern);
        final List<Span<?>> beginning = array.beginning;

        assertEquals(1, beginning.size());
        assertEquals("a", ((Token) beginning.get(0)).parsed());
    }

    @Test
    public void parseArray_readsMultipleKeys() throws JelException {
        final DestructurePattern pattern = parse("[ a, b, c ]");

        final List<Span<?>> beginning = ((ArrayDestructurePattern) pattern).beginning;
        assertEquals(3, beginning.size());
        assertEquals("a", ((Token) beginning.get(0)).parsed());
        assertEquals("b", ((Token) beginning.get(1)).parsed());
        assertEquals("c", ((Token) beginning.get(2)).parsed());
    }

    @Test
    public void parseArray_readsEllipseOperator() throws JelException {
        final DestructurePattern pattern = parse("[ a .. b, c ]");

        final List<Span<?>> beginning = ((ArrayDestructurePattern) pattern).beginning;
        assertEquals(1, beginning.size());
        assertEquals("a", ((Token) beginning.get(0)).parsed());

        final List<Span<?>> end = ((ArrayDestructurePattern) pattern).end;
        assertEquals(2, end.size());
        assertEquals("b", ((Token) end.get(0)).parsed());
        assertEquals("c", ((Token) end.get(1)).parsed());
    }

    @Test
    public void parseArray_readsRecursivePatterns() throws JelException {
        final DestructurePattern pattern = parse("[ { a } .. [ b ] ]");

        final List<Span<?>> beginning = ((ArrayDestructurePattern) pattern).beginning;
        assertEquals(1, beginning.size());
        assertInstanceOf(ObjectDestructurePattern.class, beginning.get(0));

        final List<Span<?>> end = ((ArrayDestructurePattern) pattern).end;
        assertEquals(1, end.size());
        assertInstanceOf(ArrayDestructurePattern.class, end.get(0));
    }

    @Test
    public void parseArray_toleratesWhitespaceAndComments() throws JelException {
        final DestructurePattern pattern = parse("[ a \n b // comment \n c ]");

        final List<Span<?>> beginning = ((ArrayDestructurePattern) pattern).beginning;
        assertEquals(3, beginning.size());
        assertEquals("a", ((Token) beginning.get(0)).parsed());
        assertEquals("b", ((Token) beginning.get(1)).parsed());
        assertEquals("c", ((Token) beginning.get(2)).parsed());
    }

    @Test
    public void parseArray_doesNotTolerate_incompleteEllipse() {
        final String text = "[ a . b ]";
        final String message = """
            JelException: Illegal operator
            ----------------------------------------------------
                0 | [ a . b ]
                        ^
            ----------------------------------------------------
            Hint: should match the following pattern: [ a .. b ]""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parseArray_doesNotTolerate_duplicateEllipse() {
        final String text = "[ a .. b .. c ]";
        final String message = """
            JelException: Redundant operator
            ----------------------------------------------------
                0 | [ a .. b .. c ]
                             ^^
            ----------------------------------------------------
            Hint: should match the following pattern: [ a .. b ]""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parseArray_doesNotTolerateExtraneousTokens() {
        final String text = "[ a, 1234, c ]";
        final String message = """
            JelException: Expected identifier, nested pattern, or '..'
            ----------------------------------------------------------
                0 | [ a, 1234, c ]
                         ^^^^
            ----------------------------------------------------------
            Hint: should be one of: [ a, [b], {c}, .. d ]""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    private static DestructurePattern parse(final String text) throws JelException {
        return PARSER.parse((ContainerToken) Tokenizer.containerize(text).get(0));
    }
}
