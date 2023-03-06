package xjs.jel.expression;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.StringType;
import xjs.jel.exception.JelException;
import xjs.serialization.Span;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.Token;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class StringExpressionTest {

    @Test
    public void apply_concatenatesStrings() throws JelException {
        final Expression exp =
            exp(string("Hello, "), string("world!"));

        assertEquals(Json.value("Hello, world!"), exp.apply(null));
    }

    @Test
    public void apply_appendsTextOfStringValue() throws JelException {
        final Expression exp =
            exp(string("Hello, "), LiteralExpression.of("world!"));

        assertEquals(Json.value("Hello, world!"), exp.apply(null));
    }

    @Test
    public void apply_appendsValueToString() throws JelException {
        final Expression exp =
            exp(string("result: "), array(1, 2, 3));

        assertEquals(Json.value("result: [1,2,3]"), exp.apply(null));
    }

    private static StringExpression exp(final Span<?>... subs) {
        return new StringExpression(List.of(subs));
    }

    private static Token string(final String string) {
        return new StringToken(StringType.NONE, string);
    }

    private static LiteralExpression array(final Object... elements) {
        return LiteralExpression.of(Json.any(elements));
    }
}
