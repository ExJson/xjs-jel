package xjs.jel.serialization.sequence;

import org.junit.jupiter.api.Test;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ArrayExpression;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.expression.ObjectExpression;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.expression.StringExpression;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.Tokenizer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ValueParserTest {

    private static final ValueParser PARSER = Sequencer.JEL.valueParser;

    @Test
    public void parse_identifiesSimpleLiteralExpression() throws JelException {
        final Expression exp = parse("1234");

        assertInstanceOf(LiteralExpression.OfNumber.class, exp);
        assertEquals(1234, exp.applyAsNumber(null));
        assertEquals(0, ((LiteralExpression) exp).start());
        assertEquals(4, ((LiteralExpression) exp).end());
    }

    @Test
    public void parse_paresEmptyContainer_asVoidString() throws JelException {
        final Expression exp = parse("");

        assertInstanceOf(LiteralExpression.OfString.class, exp);
        assertEquals("", exp.applyAsString(null));
        assertEquals(0, ((LiteralExpression) exp).start());
        assertEquals(0, ((LiteralExpression) exp).end());
    }

    @Test
    public void parse_identifiesReferenceExpression() throws JelException {
        final Expression exp = parse("$path[0].test..");

        assertInstanceOf(ReferenceExpression.class, exp);
        assertEquals(0, ((ReferenceExpression) exp).start());
        assertEquals(15, ((ReferenceExpression) exp).end());
    }

    @Test
    public void parse_identifiesStringExpression() throws JelException {
        final Expression exp = parse("hello $world");

        assertInstanceOf(StringExpression.class, exp);
        assertEquals(0, ((StringExpression) exp).start());
        assertEquals(12, ((StringExpression) exp).end());
    }

    @Test
    public void parse_toleratesAdditionalTokens_afterReferenceExpression() throws JelException {
        final Expression exp = parse("$path^");

        final StringExpression str = assertInstanceOf(StringExpression.class, exp);
        assertEquals(0, str.start());
        assertEquals(6, str.end());

        assertEquals(2, str.size());
        assertInstanceOf(ReferenceExpression.class, str.spans().get(0));
        assertInstanceOf(ParsedToken.class, str.spans().get(1));
    }

    @Test
    public void parse_identifiesObject() throws JelException {
        assertInstanceOf(ObjectExpression.class, parse("{}"));
    }

    @Test
    public void parse_doesNotTolerate_symbolsAfterObject() {
        assertThrows(JelException.class, () -> parse("{} more"));
    }

    @Test
    public void parseInlined_doesTolerate_symbolsAfterObject() {
        assertDoesNotThrow(() -> parseInlined("{} more"));
    }

    @Test
    public void parse_identifiesArray() throws JelException {
        assertInstanceOf(ArrayExpression.class, parse("[]"));
    }

    @Test
    public void parse_doesNotTolerate_symbolsAfterArray() {
        assertThrows(JelException.class, () -> parse("[] more"));
    }

    @Test
    public void parseInlined_doesTolerate_symbolsAfterArray() {
        assertDoesNotThrow(() -> parseInlined("[] more"));
    }

    @Test
    public void parse_identifiesOperatorExpression() throws JelException {
        // todo
    }

    @Test
    public void parse_inspectsSingleStringToken() throws JelException {
        final Expression exp = parse("'formatted $string'");

        final StringExpression str = assertInstanceOf(StringExpression.class, exp);
        assertEquals(1, str.start());
        assertEquals(18, str.end());

        assertEquals(2, str.size());
        assertInstanceOf(ParsedToken.class, str.spans().get(0));
        assertInstanceOf(ReferenceExpression.class, str.spans().get(1));
    }

    private static Expression parse(final String text) throws JelException {
        final ContainerToken tokens = Tokenizer.containerize(text);
        return PARSER.parse(tokens.iterator(), tokens.size());
    }

    private static Expression parseInlined(final String text) throws JelException {
        final ContainerToken tokens = Tokenizer.containerize(text);
        return PARSER.parse(tokens.iterator(), tokens.size(), true);
    }
}
