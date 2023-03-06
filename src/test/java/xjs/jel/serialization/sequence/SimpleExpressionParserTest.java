package xjs.jel.serialization.sequence;

import org.junit.jupiter.api.Test;
import xjs.comments.CommentType;
import xjs.core.JsonValue;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.expression.StringExpression;
import xjs.jel.expression.TupleExpression;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;
import xjs.serialization.token.Tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SimpleExpressionParserTest {

    private static final SimpleExpressionParser PARSER =
        Sequencer.JEL.expressionParser;

    @Test
    public void literalPrimitive_parsesLiteralNumber() throws JelException {
        final LiteralExpression exp = literal("1234");

        assertInstanceOf(LiteralExpression.OfNumber.class, exp);
        assertEquals(1234, exp.applyAsNumber(null));
        assertEquals(0, exp.start());
        assertEquals(4, exp.end());
    }

    @Test
    public void literalPrimitive_parsesLiteralKeyword() throws JelException {
        final LiteralExpression exp = literal("true");

        assertInstanceOf(LiteralExpression.OfBoolean.class, exp);
        assertTrue(exp.applyAsBoolean(null));
        assertEquals(0, exp.start());
        assertEquals(4, exp.end());
    }

    @Test
    public void literalPrimitive_parsesQuotedString() throws JelException {
        final LiteralExpression exp = literal("'text'");

        assertInstanceOf(LiteralExpression.OfString.class, exp);
        assertEquals("text", exp.applyAsString(null));
        assertEquals(0, exp.start());
        assertEquals(6, exp.end());
    }

    @Test
    public void literalPrimitive_parsesImplicitString() throws JelException {
        final LiteralExpression exp = literal("implicit string");

        assertInstanceOf(LiteralExpression.OfString.class, exp);
        assertEquals("implicit string", exp.applyAsString(null));
        assertEquals(0, exp.start());
        assertEquals(15, exp.end());
    }


    @Test
    public void stringExpression_parsesFormattedString() throws JelException {
        final StringExpression exp = formatted("a $b c");

        assertEquals(3, exp.size());
        assertEquals("a ", ((Token) exp.spans().get(0)).parsed());
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(1));
        assertEquals(" c", ((Token) exp.spans().get(2)).parsed());
    }

    @Test
    public void stringExpression_parsesUnFormattedString() throws JelException {
        final StringExpression exp = formatted("a b c");

        assertEquals(1, exp.size());
        assertEquals("a b c", ((Token) exp.spans().get(0)).parsed());
    }

    @Test
    public void stringExpression_parsesReferenceOnly() throws JelException {
        final StringExpression exp = formatted("$a.b.c");

        assertEquals(1, exp.size());
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(0));
    }

    @Test
    public void stringExpression_preservesWhitespace_betweenReferences() throws JelException {
        final StringExpression exp = formatted("$a \t $b");

        assertEquals(3, exp.size());
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(0));
        assertEquals(" \t ", ((Token) exp.spans().get(1)).parsed());
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(2));
    }

    @Test
    public void stringExpression_parsesReferencesRecursively() throws JelException {
        final StringExpression exp = formatted("demo {$ref}");

        assertEquals(3, exp.size());
        assertEquals("demo {", ((Token) exp.spans().get(0)).parsed());
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(1));
        assertEquals("}", ((Token) exp.spans().get(2)).parsed());
    }

    @Test
    public void stringExpression_trimsIndentation_byFirstToken() throws JelException {
        final StringExpression exp = formatted("  demo {\n    $ref\n  }");

        assertEquals(3, exp.size());
        assertEquals("demo {\n  ", ((Token) exp.spans().get(0)).parsed());
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(1));
        assertEquals("\n}", ((Token) exp.spans().get(2)).parsed());
    }

    @Test
    public void tupleExpression_parsesTupleOfNone() throws JelException {
        assertEquals(0, tuple("()").size());
    }

    @Test
    public void tupleExpression_parsesTupleOfOne() throws JelException {
        final TupleExpression exp = tuple("(element)");

        assertEquals(1, exp.size());
        assertEquals("element", ((Expression) exp.spans().get(0)).applyAsString(null));
    }

    @Test
    public void tupleExpression_parsesTupleOfMany() throws JelException {
        final TupleExpression exp = tuple("(1, 2, 3)");

        assertEquals(3, exp.size());
        assertEquals(1, ((Expression) exp.spans().get(0)).applyAsNumber(null));
        assertEquals(2, ((Expression) exp.spans().get(1)).applyAsNumber(null));
        assertEquals(3, ((Expression) exp.spans().get(2)).applyAsNumber(null));
    }

    @Test
    public void tupleExpression_toleratesBreaksAsDelimiters() throws JelException {
        final TupleExpression exp = tuple("(\n1\n\n2\n\n3\n)");

        assertEquals(4, exp.size());
        assertEquals(1, ((Expression) exp.spans().get(0)).applyAsNumber(null));
        assertEquals(2, ((Expression) exp.spans().get(1)).applyAsNumber(null));
        assertEquals(3, ((Expression) exp.spans().get(2)).applyAsNumber(null));
        assertEquals(TokenType.BREAK, ((Token) exp.spans().get(3)).type());
    }

    @Test
    public void tupleExpression_toleratesMetadata() throws JelException {
        final TupleExpression exp = tuple("( 1 // comment \n , 2 )");

        assertEquals(2, exp.size());
        final JelMember member = assertInstanceOf(JelMember.class, exp.spans().get(0));
        final JsonValue formatting = member.getFormatting();
        assertEquals("comment", formatting.getComment(CommentType.EOL));
        assertEquals(1, member.applyAsNumber(null));
        assertEquals(2, ((Expression) exp.spans().get(1)).applyAsNumber(null));
    }

    private static LiteralExpression literal(final String text) {
        final ContainerToken tokens = Tokenizer.containerize(text);
        return PARSER.literalPrimitive(tokens.iterator(), tokens.size());
    }

    private static StringExpression formatted(
            final String text) throws JelException {
        final ContainerToken tokens = Tokenizer.containerize(text);
        return PARSER.stringExpression(tokens.iterator(), tokens.size());
    }

    private static TupleExpression tuple(final String text) throws JelException {
        final ContainerToken tokens = Tokenizer.containerize(text);
        return PARSER.tupleExpression((ContainerToken) tokens.get(0));
    }
}
