package xjs.jel.expression;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.ModifyingOperator;
import xjs.jel.sequence.ModifyingOperatorSequence;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.SymbolToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static xjs.jel.sequence.Operator.ADD;
import static xjs.jel.sequence.Operator.MULTIPLY;
import static xjs.jel.sequence.Operator.RIGHT_SHIFT;
import static xjs.jel.sequence.ModifyingOperator.INVERT;

public final class ArithmeticExpressionTest {

    @Test
    public void apply_evaluatesSingleOperator() throws JelException {
        final Expression exp =
            exp(num(2), op(ADD), num(2));

        assertEquals(Json.value(4), exp.apply(null));
    }

    @Test
    public void apply_evaluatesDoubleOperator() throws JelException {
        final Expression exp =
            exp(num(2), op(RIGHT_SHIFT), num(3));

        assertEquals(Json.value(2 >> 3), exp.apply(null));
    }

    @Test
    public void apply_evaluatesChainedOperators() throws JelException {
        final Expression exp =
            exp(num(1), op(ADD), num(2), op(ADD), num(3));

        assertEquals(Json.value(6), exp.apply(null));
    }

    @Test
    public void apply_followsOrderOfOperations() throws JelException {
        final Expression exp =
            exp(num(1), op(ADD), num(2), op(MULTIPLY), num(3));

        assertEquals(Json.value(7), exp.apply(null));
    }

    @Test
    public void apply_evaluatesParenthesesFirst() throws JelException {
        final Expression exp =
            exp(
                exp(num(1), op(ADD), num(2)),
                op(MULTIPLY), num(3));

        assertEquals(Json.value(9), exp.apply(null));
    }

    @Test
    public void apply_toleratesNegativeSign_asSymbolToken() throws JelException {
        final Expression exp =
            exp(modOp(INVERT), num(3), op(MULTIPLY), modOp(INVERT), num(3), op(MULTIPLY), num(-1));

        assertEquals(Json.value(-9), exp.apply(null));
    }

    private static ArithmeticExpression exp(final Sequence<?>... subs) {
        return new ArithmeticExpression(List.of(subs), false);
    }

    private static LiteralExpression num(final double number) {
        return LiteralExpression.of(number);
    }

    private static ModifyingOperatorSequence modOp(final ModifyingOperator op) {
        return new ModifyingOperatorSequence(op, new SymbolToken('?'));
    }

    private static OperatorSequence op(final Operator op) {
        return new OperatorSequence(op, 0);
    }
}
