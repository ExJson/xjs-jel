package xjs.jel.expression;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonLiteral;
import xjs.jel.Alias;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.TokenType;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ConditionalExpressionTest {

    private static final JelContext CTX = new JelContext(new File(""));

    @Test
    public void apply_returnsFirstBranch_whereConditionIsMet() throws JelException {
        final Expression exp = exp(
            branch(bool(false), num(1)),
            branch(bool(false), num(2)),
            branch(bool(true), num(3)));

        assertTrue(Json.value(3).matches(exp.apply(CTX)));
    }

    @Test
    public void apply_returnsNull_whenNoConditionIsMet() throws JelException {
        final Expression exp =
            exp(branch(bool(false), num(1)));

        assertEquals(JsonLiteral.jsonNull(), exp.apply(CTX));
    }

    private static ConditionalExpression exp(final JelMember... subs) {
        return new ConditionalExpression(
            new ContainerToken(TokenType.OPEN, List.of()),
            List.of(subs));
    }

    private static JelMember branch(final Expression condition, final Expression out) {
        return JelMember.builder(JelType.MEMBER)
            .alias(Alias.of(condition))
            .expression(out)
            .build();
    }

    private static LiteralExpression num(final double number) {
        return LiteralExpression.of(number);
    }

    private static LiteralExpression bool(final boolean bool) {
        return LiteralExpression.of(bool);
    }
}
