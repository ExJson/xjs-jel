package xjs.jel.expression;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonObject;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.ModifyingOperator;
import xjs.jel.sequence.ModifyingOperatorSequence;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.SymbolToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static xjs.jel.sequence.Operator.AND;
import static xjs.jel.sequence.Operator.EQUAL_TO;
import static xjs.jel.sequence.Operator.GREATER_THAN;
import static xjs.jel.sequence.Operator.GREATER_THAN_EQUAL_TO;
import static xjs.jel.sequence.Operator.LESS_THAN;
import static xjs.jel.sequence.Operator.LESS_THAN_EQUAL_TO;
import static xjs.jel.sequence.Operator.NOT_EQUAL_TO;
import static xjs.jel.sequence.Operator.OR;

public final class BooleanExpressionTest {

    @Test
    public void apply_comparesGreaterThan() throws JelException {
        final Expression trueExp =
            exp(num(3), op(GREATER_THAN), num(1));

        final Expression falseExp =
            exp(num(1), op(GREATER_THAN), num(3));

        assertEquals(Json.value(true), trueExp.apply(null));
        assertEquals(Json.value(false), falseExp.apply(null));
    }

    @Test
    public void apply_comparesGreaterThan_orEqualTo() throws JelException {
        final Expression trueExp =
            exp(num(5), op(GREATER_THAN_EQUAL_TO), num(5));

        final Expression falseExp =
            exp(num(2), op(GREATER_THAN_EQUAL_TO), num(4));

        assertEquals(Json.value(true), trueExp.apply(null));
        assertEquals(Json.value(false), falseExp.apply(null));
    }

    @Test
    public void apply_comparesLessThan() throws JelException {
        final Expression trueExp =
            exp(num(1), op(LESS_THAN), num(3));

        final Expression falseExp =
            exp(num(3), op(LESS_THAN), num(1));

        assertEquals(Json.value(true), trueExp.apply(null));
        assertEquals(Json.value(false), falseExp.apply(null));
    }

    @Test
    public void apply_comparesLessThan_orEqualTo() throws JelException {
        final Expression trueExp =
            exp(num(5), op(LESS_THAN_EQUAL_TO), num(5));

        final Expression falseExp =
            exp(num(4), op(LESS_THAN_EQUAL_TO), num(2));

        assertEquals(Json.value(true), trueExp.apply(null));
        assertEquals(Json.value(false), falseExp.apply(null));
    }

    @Test
    public void apply_comparesEqualTo() throws JelException {
        final Expression trueExp =
            exp(object("a", 1), op(EQUAL_TO), object("a", 1));

        final Expression falseExp =
            exp(object("a", 1), op(EQUAL_TO), object("b", 2));

        assertEquals(Json.value(true), trueExp.apply(null));
        assertEquals(Json.value(false), falseExp.apply(null));
    }

    @Test
    public void apply_comparesNotEqualTo() throws JelException {
        final Expression trueExp =
            exp(object("a", 1), op(NOT_EQUAL_TO), object("b", 2));

        final Expression falseExp =
            exp(object("a", 1), op(NOT_EQUAL_TO), object("a", 1));

        assertEquals(Json.value(true), trueExp.apply(null));
        assertEquals(Json.value(false), falseExp.apply(null));
    }

    @Test
    public void apply_comparesAndOperator() throws JelException {
        final Expression trueExp =
            exp(bool(true), op(AND), bool(true));

        final Expression falseExp =
            exp(bool(true), op(AND), bool(false));

        assertEquals(Json.value(true), trueExp.apply(null));
        assertEquals(Json.value(false), falseExp.apply(null));
    }

    @Test
    public void apply_comparesOrOperator() throws JelException {
        final Expression trueExp =
            exp(bool(false), op(OR), bool(true));

        final Expression falseExp =
            exp(bool(false), op(OR), bool(false));

        assertEquals(Json.value(true), trueExp.apply(null));
        assertEquals(Json.value(false), falseExp.apply(null));
    }

    @Test
    public void apply_coercesSides_intoBooleans() throws JelException {
        final Expression trueExp =
            exp(num(1), op(AND), num(2));

        final Expression falseExp =
            exp(num(0), op(AND), num(1));

        assertEquals(Json.value(true), trueExp.apply(null));
        assertEquals(Json.value(false), falseExp.apply(null));
    }

    @Test
    public void apply_toleratesChainedOperators() throws JelException {
        final Expression exp = exp(
            bool(true), op(AND), bool(false), op(OR), bool(true));

        assertEquals(Json.value(true), exp.apply(null));
    }

    @Test
    public void apply_comparesRelationalOperators_first() throws JelException {
        final Expression exp = exp(
            num(1), op(LESS_THAN), num(2),
            op(AND),
            num(3), op(GREATER_THAN), num(0));

        assertEquals(Json.value(true), exp.apply(null));
    }

    private static BooleanExpression exp(final Sequence<?>... subs) {
        return new BooleanExpression(List.of(subs));
    }

    private static LiteralExpression num(final double number) {
        return LiteralExpression.of(number);
    }

    private static OperatorSequence op(final Operator op) {
        return new OperatorSequence(op, 0);
    }

    private static ModifyingOperatorSequence modOp(final ModifyingOperator op) {
        return new ModifyingOperatorSequence(op, new SymbolToken('?'));
    }

    private static LiteralExpression bool(final boolean bool) {
        return LiteralExpression.of(bool);
    }

    private static LiteralExpression object(final Object... kvs) {
        final JsonObject o = new JsonObject();
        for (int i = 0; i < kvs.length; i += 2) {
            o.add(kvs[i].toString(), Json.any(kvs[i + 1]));
        }
        return LiteralExpression.of(o);
    }
}
