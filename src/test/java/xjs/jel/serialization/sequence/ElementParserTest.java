package xjs.jel.serialization.sequence;

import org.junit.jupiter.api.Test;
import xjs.comments.CommentType;
import xjs.core.JsonValue;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.serialization.Span;
import xjs.serialization.token.Tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public final class ElementParserTest {

    private static final ElementParser PARSER = Sequencer.JEL.elementParser;

    @Test
    public void parse_parsesValueIntoMember() throws JelException {
        final JelMember member = parse("1234");
        final Expression exp = member.getExpression();

        assertInstanceOf(LiteralExpression.OfNumber.class, exp);
        assertEquals(1234, member.applyAsNumber(null));
        assertEquals(0, member.start());
        assertEquals(4, member.end());
    }

    @Test
    public void parse_capturesWhitespaceAndComments() throws JelException {
        final JelMember member = parse("// c1 \n1234 // c2");
        final Expression exp = member.getExpression();
        final JsonValue formatting = member.getFormatting();

        assertInstanceOf(LiteralExpression.OfNumber.class, exp);
        assertEquals(1234, member.applyAsNumber(null));
        assertEquals(0, member.start());
        assertEquals(17, member.end());

        assertEquals("c1", formatting.getComment(CommentType.HEADER));
        assertEquals("c2", formatting.getComment(CommentType.EOL));
    }

    @Test
    public void parse_stopsAtFirstNewLine_afterValue() throws JelException {
        final JelMember member = parse("hello world \n more tokens");

        assertEquals("hello world", member.applyAsString(null));
        assertEquals(0, member.start());
        assertEquals(11, member.end());
    }

    @Test
    public void parse_stopsAtFirstComma_afterValue() throws JelException {
        final JelMember member = parse("hello world, more tokens");

        assertEquals("hello world", member.applyAsString(null));
        assertEquals(0, member.start());
        assertEquals(11, member.end());
    }

    @Test
    public void parse_withNoSignificantTokens_beforeComma_returnsEmptyString() throws JelException {
        final JelMember member = parse("/* comment */ \n /* comment */ \n ,");
        final Expression exp = member.getExpression();

        assertEquals("", member.applyAsString(null));
        assertEquals(0, member.start());
        assertEquals(32, member.end());
        assertEquals(32, ((Span<?>) exp).start());
        assertEquals(32, ((Span<?>) exp).end());
    }

    @Test
    public void parse_withNoSignificantTokens_returnsEmptyString() throws JelException {
        final JelMember member = parse("/* comment */ \n /* comment */ \n");
        final Expression exp = member.getExpression();

        assertEquals("", member.applyAsString(null));
        assertEquals(0, member.start());
        assertEquals(31, member.end());
        assertEquals(0, ((Span<?>) exp).start());
        assertEquals(0, ((Span<?>) exp).end());
    }

    @Test
    public void parse_withValueModifier_parsesModifiedValueType() throws JelException {
        // todo
    }

    @Test
    public void parseInline_withOnlySignificantTokens_returnsSimpleExpression() throws JelException {
        final Expression exp = parseInline("1234");

        assertInstanceOf(LiteralExpression.OfNumber.class, exp);
        assertEquals(1234, exp.applyAsNumber(null));
        assertEquals(0, ((Span<?>) exp).start());
        assertEquals(4, ((Span<?>) exp).end());
    }

    @Test
    public void parseInline_withNonSignificantTokens_returnsMember() throws JelException {
        final Expression exp = parseInline("// c1 \n1234 // c2");

        assertInstanceOf(JelMember.class, exp);
        assertEquals(1234, exp.applyAsNumber(null));
        assertEquals(0, ((Span<?>) exp).start());
        assertEquals(17, ((Span<?>) exp).end());
    }

    private static JelMember parse(final String text) throws JelException {
        return PARSER.parse(Tokenizer.containerize(text).iterator());
    }

    private static Expression parseInline(final String text) throws JelException {
        return PARSER.parseInline(Tokenizer.containerize(text).iterator());
    }
}
