package xjs.jel.serialization.sequence;

import org.junit.jupiter.api.Test;
import xjs.comments.CommentType;
import xjs.core.Json;
import xjs.core.JsonFormat;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ArrayExpression;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Tokenizer;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ArrayParserTest {

    private static final ArrayParser PARSER = Sequencer.JEL.arrayParser;
    private static final JelContext CTX = new JelContext(new File(""));

    @Test
    public void parse_readsEmptyArray() throws JelException {
        final ArrayExpression exp = parse("[]");

        assertEquals(Json.array(), exp.apply(CTX));
        assertEquals(0, exp.start());
        assertEquals(2, exp.end());
    }

    @Test
    public void parse_readsSingleElement() throws JelException {
        final ArrayExpression exp = parse("[1234]");

        assertTrue(Json.array(1234).matches(exp.apply(CTX)));
        assertEquals(0, exp.start());
        assertEquals(6, exp.end());
    }

    @Test
    public void parse_readsMultipleElements() throws JelException {
        final ArrayExpression exp = parse("[1,2,3]");

        assertTrue(Json.array(1, 2, 3).matches(exp.apply(CTX)));
        assertEquals(0, exp.start());
        assertEquals(7, exp.end());
    }

    @Test
    public void parse_toleratesTrailingComma() throws JelException {
        final ArrayExpression exp = parse("[1,2,]");

        assertTrue(Json.array(1, 2).matches(exp.apply(CTX)));
        assertEquals(0, exp.start());
        assertEquals(6, exp.end());
    }

    @Test
    public void parse_readsVoidStrings() throws JelException {
        final ArrayExpression exp = parse("[,,,]");

        assertTrue(Json.array("", "", "").matches(exp.apply(CTX)));
        assertEquals(0, exp.start());
        assertEquals(5, exp.end());
    }

    @Test
    public void parse_toleratesMetadata_afterTrailingComma() throws JelException {
        final ArrayExpression exp = parse("[,\n/*c*/]");

        assertTrue(Json.array("").matches(exp.apply(CTX)));
        assertEquals(0, exp.start());
        assertEquals(9, exp.end());
    }

    @Test
    public void parse_readsNewlines_asDelimiters() throws JelException {
        final ArrayExpression exp = parse("[1\n2\n3]");

        assertTrue(Json.array(1, 2, 3).matches(exp.apply(CTX)));
        assertEquals(0, exp.start());
        assertEquals(7, exp.end());
    }

    @Test
    public void parse_toleratesMultipleNewlines_betweenValues() throws JelException {
        final ArrayExpression exp = parse("[1\n\n2\n\n3]");

        assertTrue(Json.array(1, 2, 3).matches(exp.apply(CTX)));
        assertEquals(0, exp.start());
        assertEquals(9, exp.end());
    }

    @Test
    public void parse_readsNewlineAfterComment_asDelimiter() throws JelException {
        final ArrayExpression exp = parse("[1//c\n2]");

        assertTrue(Json.array(1, 2).matches(exp.apply(CTX)));
        assertEquals(0, exp.start());
        assertEquals(8, exp.end());
    }

    @Test
    public void parse_preservesBasicFormatting() throws JelException {
        final String text = """
            [
              1 // eol
              // header
              2
              
              3
              
              // interior
            ]""".replaceAll("\r?\n", System.lineSeparator());
        assertEquals(text, parse(text).apply(CTX).toString(JsonFormat.XJS_FORMATTED));
    }

    @Test
    public void parse_withCommentAfterComma_parsesCommentAsEol() throws JelException {
        final ArrayExpression exp = parse("[ 1, // eol \n 2 ]");

        assertEquals(2, exp.size());

        final JelMember e1 = assertInstanceOf(JelMember.class, exp.spans().get(0));
        assertEquals("eol", e1.getFormatting().getComment(CommentType.EOL));
        assertEquals(1, e1.applyAsNumber(null));

        final JelMember e2 = assertInstanceOf(JelMember.class, exp.spans().get(1));
        assertFalse(e2.getFormatting().hasComments());
        assertEquals(2, e2.applyAsNumber(null));
    }

    private static ArrayExpression parse(final String text) throws JelException {
        final ContainerToken root = Tokenizer.containerize(text);
        return PARSER.parse((ContainerToken) root.get(0));
    }
}
