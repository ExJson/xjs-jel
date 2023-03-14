package xjs.jel.expression;

import org.jetbrains.annotations.Nullable;
import xjs.core.Json;
import xjs.core.JsonType;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.ModifyingOperator;
import xjs.jel.sequence.ModifyingOperatorSequence;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.jel.sequence.OperatorType;
import xjs.jel.sequence.Sequence;

import java.util.List;

public class BooleanExpression extends OperatorExpression {

    public BooleanExpression(final List<Sequence<?>> subs) {
        super(JelType.BOOLEAN_EXPRESSION, subs);
    }

    protected BooleanExpression(final JelType type, final List<Sequence<?>> subs) {
        super(type, subs);
    }

    @Override
    public @Nullable JsonType getStrongType() {
        return JsonType.BOOLEAN;
    }

    @Override
    protected JsonValue calculate(
            final JelContext ctx, final Sequence<Sequence<?>>.Itr itr) throws JelException {
        boolean out = getNextBoolean(ctx, itr);
        while (itr.hasNext()) {
            out = applyAsBoolean(this.getNextOperator(ctx, itr), out, this.getNextBoolean(ctx, itr));
        }
        return Json.value(out);
    }

    protected boolean getNextBoolean(
            final JelContext ctx, final Sequence<Sequence<?>>.Itr itr) throws JelException {
        final Sequence<?> next = itr.next();
        if (next == null) {
            throw new JelException("no booleans").withSpan(ctx, this);
        }
        if (next instanceof ModifyingOperatorSequence) {
            final ModifyingOperatorSequence m = (ModifyingOperatorSequence) next;
            if (m.op != ModifyingOperator.NOT) {
                throw new JelException("Unsupported modifier in boolean expression")
                    .withSpan(ctx, m)
                    .withDetails("Hint: boolean expression only supports '!' modifier");
            }
            return !getNextBoolean(ctx, itr);
        }
        if (!(next instanceof Expression)) {
            throw new JelException("Illegal operand")
                .withSpan(ctx, next);
        }
        final Operator relational = getRelationalOperator(itr);
        if (relational != null) {
            final JsonValue value = ((Expression) next).apply(ctx);
            return applyRelational(relational, value, getNextValue(ctx, itr));
        }
        return ((Expression) next).applyAsBoolean(ctx);
    }

    private static @Nullable Operator getRelationalOperator(final Sequence<Sequence<?>>.Itr itr) {
        final Sequence<?> peek = itr.peek();
        if (peek instanceof OperatorSequence) {
            final Operator op = ((OperatorSequence) peek).op;
            if (op.type == OperatorType.RELATIONAL) {
                itr.next();
                return op;
            }
        }
        return null;
    }

    private static boolean applyAsBoolean(
            final Operator op, final boolean lhs, final boolean rhs) {
        switch (op) {
            case OR: return lhs || rhs;
            case AND: return lhs && rhs;
        }
        return false;
    }

    private static boolean applyRelational(
            final Operator op, final JsonValue lhs, final JsonValue rhs) {
        switch (op) {
            case GREATER_THAN: return lhs.intoDouble() > rhs.intoDouble();
            case GREATER_THAN_EQUAL_TO: return lhs.intoDouble() >= rhs.intoDouble();
            case LESS_THAN: return lhs.intoDouble() < rhs.intoDouble();
            case LESS_THAN_EQUAL_TO: return lhs.intoDouble() <= rhs.intoDouble();
            case EQUAL_TO: return lhs.matches(rhs);
            case NOT_EQUAL_TO: return !lhs.matches(rhs);
        }
        return false;
    }
}
