package xjs.jel.serialization.sequence;

import org.junit.jupiter.api.Test;
import xjs.comments.CommentType;
import xjs.core.Json;
import xjs.core.JsonFormat;
import xjs.core.JsonObject;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ObjectExpression;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Tokenizer;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ObjectParserTest {

    private static final ObjectParser PARSER = Sequencer.JEL.objectParser;
    private static final JelContext CTX = new JelContext(new File(""));

    @Test
    public void parse_readsEmptyObject() throws JelException {
        assertEquals(Json.object(), parse("{}").apply(CTX));
    }

    @Test
    public void parse_readsSingleMember() throws JelException {
        final ObjectExpression exp = parse("{a: 1}");

        assertTrue(Json.object().add("a", 1).matches(exp.apply(CTX)));
    }

    @Test
    public void parse_readsMultipleMembers() throws JelException {
        final ObjectExpression exp = parse("{a:1,b:2,c:3}");
        final JsonObject expected = Json.object()
            .add("a", 1).add("b", 2).add("c", 3);

        assertTrue(expected.matches(exp.apply(CTX)));
    }

    @Test
    public void parse_toleratesTrailingComma() throws JelException {
        final ObjectExpression exp = parse("{a:1,b:2,c:3,}");
        final JsonObject expected = Json.object()
            .add("a", 1).add("b", 2).add("c", 3);

        assertTrue(expected.matches(exp.apply(CTX)));
    }

    @Test
    public void parse_readsVoidStrings() throws JelException {
        final ObjectExpression exp = parse("{:,a:,b:}");
        final JsonObject expected = Json.object()
            .add("", "").add("a", "").add("b", "");

        assertTrue(expected.matches(exp.apply(CTX)));
    }

    @Test
    public void parse_toleratesMetadata_afterTrailingComma() throws JelException {
        final ObjectExpression exp = parse("{:,\n/*c*/}");
        final JsonObject expected = Json.object().add("", "");

        assertTrue(expected.matches(exp.apply(CTX)));
    }

    @Test
    public void parse_readsNewlines_asDelimiters() throws JelException {
        final ObjectExpression exp = parse("{a:1\nb:2\nc:3}");
        final JsonObject expected = Json.object()
            .add("a", 1).add("b", 2).add("c", 3);

        assertTrue(expected.matches(exp.apply(CTX)));
    }

    @Test
    public void parse_toleratesMultipleNewlines_betweenValues() throws JelException {
        final ObjectExpression exp = parse("{a:1\n\nb:2\n\nc:3}");
        final JsonObject expected = Json.object()
            .add("a", 1).add("b", 2).add("c", 3);

        assertTrue(expected.matches(exp.apply(CTX)));
    }

    @Test
    public void parse_readsNewlineAfterComment_asDelimiter() throws JelException {
        final ObjectExpression exp = parse("{a:1//c\nb:2}");
        final JsonObject expected = Json.object()
            .add("a", 1).add("b", 2);

        assertTrue(expected.matches(exp.apply(CTX)));
    }

    @Test
    public void parse_preservesBasicFormatting() throws JelException {
        final String text = """
            
            a: 1 // eol
              
            // header
            b: 2
              
            c:
              // value
              3
              
            // interior
            """.replaceAll("\r?\n", System.lineSeparator());
        assertEquals(text, parse(text).apply(CTX).toString(JsonFormat.XJS_FORMATTED));
    }

    @Test
    public void parse_withCommentAfterComma_parsesCommentAsEol() throws JelException {
        final ObjectExpression exp = parse("{ a: 1, // eol \n b: 2 }");

        assertEquals(2, exp.size());

        final JelMember e1 = assertInstanceOf(JelMember.class, exp.spans().get(0));
        assertEquals("eol", e1.getFormatting().getComment(CommentType.EOL));
        assertEquals(1, e1.applyAsNumber(null));

        final JelMember e2 = assertInstanceOf(JelMember.class, exp.spans().get(1));
        assertFalse(e2.getFormatting().hasComments());
        assertEquals(2, e2.applyAsNumber(null));
    }

    @Test
    public void parse_readsModifiers() throws JelException {
        final ObjectExpression exp = parse("""
            a >> private: not visible
            b >> noinline: $a
            """);
        final JsonObject expected = Json.object().add("b", "$a");

        assertTrue(expected.matches(exp.apply(CTX)));
    }

    // while easier to do here, most of this logic should be tested directly
    // in other unit test classes, or using this pattern in an integration test.
    @Test
    public void parse_thenApply_doesEvaluateExpressions() throws JelException {
        final ObjectExpression exp = parse("""
            a >> (a) (b): $a$b
            b: $a('hello, ')('world')!
            """);
        final JsonObject expected =
            Json.object().add("b", "hello, world!");

        assertTrue(expected.matches(exp.apply(CTX)));
    }

    private static ObjectExpression parse(final String text) throws JelException {
        ContainerToken root = Tokenizer.containerize(text);
        if (root.get(0) instanceof ContainerToken) {
            root = (ContainerToken) root.get(0);
        }
        return PARSER.parse(root);
    }
}
