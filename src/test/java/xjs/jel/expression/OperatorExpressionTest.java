package xjs.jel.expression;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonObject;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.jel.sequence.Sequence;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static xjs.jel.sequence.Operator.ADD;
import static xjs.jel.sequence.Operator.SUBTRACT;
import static xjs.jel.sequence.Operator.MULTIPLY;

public final class OperatorExpressionTest {

    @Test
    public void apply_addsNumbers() throws JelException {
        final Expression exp =
            exp(num(2), op(ADD), num(2));

        assertTrue(Json.value(4).matches(exp.apply(null)));
    }

    @Test
    public void apply_addsObjects() throws JelException {
        final Expression exp =
            exp(object("a", 1), op(ADD), object("b", 2));

        assertTrue(Json.object().add("a", 1).add("b", 2).matches(exp.apply(null)));
    }

    @Test
    public void apply_addsValue_toArray() throws JelException {
        final Expression exp =
            exp(array(1, 2, 3), op(ADD), num(4));

        assertTrue(Json.array(1, 2, 3, 4).matches(exp.apply(null)));
    }

    @Test
    public void apply_concatenatesStrings() throws JelException {
        final Expression exp =
            exp(string("hello"), op(ADD), string(", "), op(ADD), string("world"));

        assertTrue(Json.value("hello, world").matches(exp.apply(null)));
    }

    @Test
    public void apply_appendsToString() throws JelException {
        final Expression exp =
            exp(string("result: "), op(ADD), num(4));

        assertTrue(Json.value("result: 4").matches(exp.apply(null)));
    }

    @Test
    public void apply_subtractsNumbers() throws JelException {
        final Expression exp =
            exp(num(1), op(SUBTRACT), num(3));

        assertTrue(Json.value(-2).matches(exp.apply(null)));
    }

    @Test
    public void apply_subtractsKey_fromObject() throws JelException {
        final Expression exp = exp(
            object("k1", "v1", "k2", "v2"),
            op(SUBTRACT),
            string("k1"));

        assertTrue(Json.object().add("k2", "v2").matches(exp.apply(null)));
    }

    @Test
    public void apply_subtractsMultipleKeys_fromObject() throws JelException {
        final Expression exp = exp(
            object("a", 1, "b", 2, "c", 3, "d", 4),
            op(SUBTRACT),
            array("a", "c"));

        assertTrue(Json.object().add("b", 2).add("d", 4).matches(exp.apply(null)));
    }

    @Test
    public void apply_subtractsValue_fromArray() throws JelException {
        final Expression exp = exp(
            array(1, 2, 3),
            op(SUBTRACT),
            num(1));

        assertTrue(Json.array(2, 3).matches(exp.apply(null)));
    }

    @Test
    public void apply_subtractsArray_fromArray() throws JelException {
        final Expression exp = exp(
            array(1, 2, 3, 4),
            op(SUBTRACT),
            array(2, 4, "absent"));

        assertTrue(Json.array(1, 3).matches(exp.apply(null)));
    }

    @Test
    public void apply_multipliesNumbers() throws JelException {
        final Expression exp =
            exp(num(2), op(MULTIPLY), num(3));

        assertEquals(Json.value(6), exp.apply(null));
    }

    @Test
    public void apply_repeatsText() throws JelException {
        final Expression exp =
            exp(string("01"), op(MULTIPLY), num(3));

        assertTrue(Json.value("010101").matches(exp.apply(null)));
    }

    @Test
    public void apply_doesNotFollow_orderOfOperations() throws JelException {
        final Expression exp =
            exp(num(1), op(ADD), num(2), op(MULTIPLY), num(3));

        assertTrue(Json.value(9).matches(exp.apply(null)));
    }

    private static OperatorExpression exp(final Sequence<?>... subs) {
        return new OperatorExpression(List.of(subs));
    }

    private static LiteralExpression string(final String string) {
        return LiteralExpression.of(string);
    }

    private static LiteralExpression num(final double number) {
        return LiteralExpression.of(number);
    }

    private static OperatorSequence op(final Operator op) {
        return new OperatorSequence(op, 0);
    }

    private static LiteralExpression object(final Object... kvs) {
        final JsonObject o = new JsonObject();
        for (int i = 0; i < kvs.length; i += 2) {
            o.add(kvs[i].toString(), Json.any(kvs[i + 1]));
        }
        return LiteralExpression.of(o);
    }

    private static LiteralExpression array(final Object... elements) {
        return LiteralExpression.of(Json.any(elements));
    }
}
