package xjs.jel.expression;

import org.jetbrains.annotations.Nullable;
import xjs.core.Json;
import xjs.core.JsonType;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.ModifyingOperator;
import xjs.jel.sequence.ModifyingOperatorSequence;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.jel.sequence.Sequence;

import java.util.List;

public class ArithmeticExpression extends OperatorExpression {
    private final boolean pureMath;

    public ArithmeticExpression(final List<Sequence<?>> subs, final boolean pureMath) {
        super(subs);
        this.pureMath = pureMath;
    }

    @Override
    public @Nullable JsonType getStrongType() {
        return this.pureMath ? JsonType.NUMBER : null;
    }

    @Override
    protected JsonValue calculate(
            final JelContext ctx,
            final Sequence<Sequence<?>>.Itr itr,
            final JsonValue first) throws JelException {
        if (!this.pureMath && !first.isNumber()) {
            return super.calculate(ctx, itr, first);
        }
        double out = first.intoDouble();
        while (itr.hasNext()) {
            out = applyAsNumber(
                itr, getNextOperator(itr), out, getNextNumber(ctx, itr));
        }
        return Json.value(out);
    }

    protected static double getNextNumber(
            final JelContext ctx,
            final Sequence<Sequence<?>>.Itr itr) throws JelException {
        final Sequence<?> next = itr.next();
        if (next == null) {
            throw new JelException("no numbers");
        }
        if (next instanceof ModifyingOperatorSequence) {
            final ModifyingOperatorSequence m = (ModifyingOperatorSequence) next;
            if (m.op != ModifyingOperator.INVERT) {
                throw new JelException("Unsupported modifier in arithmetic expression")
                    .withSpan(m)
                    .withDetails("Hint: arithmetic expression only supports '-' modifier");
            }
            final Sequence<?> after = itr.next();
            if (after == null) {
                throw new JelException("missing number after sign")
                    .withSpan(next);
            } else if (!(after instanceof Expression)) {
                throw new JelException("Illegal operand")
                    .withSpan(after);
            }
            return checkPrecedent(ctx, itr, -((Expression) after).applyAsNumber(ctx));
        }
        return checkPrecedent(ctx, itr, ((Expression) next).applyAsNumber(ctx));
    }

    private static double checkPrecedent(
            final JelContext ctx,
            final Sequence<Sequence<?>>.Itr itr, final double control) throws JelException {
        final Sequence<?> peek = itr.peek();
        if (peek instanceof ArithmeticExpression) {
            itr.next(); // may be unreachable due to parsing (phantom * is inserted)
            return applyAsNumber(
                itr, Operator.MULTIPLY, control, ((Expression) peek).applyAsNumber(ctx));
        } else if (peek instanceof OperatorSequence) {
            final Operator op = ((OperatorSequence) peek).op;
            if (op != Operator.ADD
                    && op != Operator.SUBTRACT
                    && op != Operator.RIGHT_SHIFT
                    && op != Operator.LEFT_SHIFT) {
                itr.next();
                return applyAsNumber(itr, op, control, getNextNumber(ctx, itr));
            }
        }
        return control;
    }

    protected static double applyAsNumber(
            final Sequence<Sequence<?>>.Itr itr,
            final Operator op,
            final double a,
            final double b) throws JelException {
        switch (op) {
            case ADD: return a + b;
            case SUBTRACT: return a - b;
            case MULTIPLY: return a * b;
            case DIVIDE: return a / checkDivideByZero(b, itr);
            case MOD: return a % checkDivideByZero(b, itr);
            case POW: return Math.pow(a, b);
            case BITWISE_AND: return (int) a & (int) b;
            case BITWISE_OR: return (int) a | (int) b;
            case LEFT_SHIFT: return (int) a << (int) b;
            case RIGHT_SHIFT: return (int) a >> (int) b;
        }
        throw new IllegalStateException(
            "cannot apply " + op.name().toLowerCase() + " as number");
    }

    protected static double checkDivideByZero(
            final double b, final Sequence<Sequence<?>>.Itr itr) throws JelException {
        if (b == 0) {
            final Sequence<?> bs = itr.peek(0);
            throw new JelException("Expression divides by zero")
                .withSpan(bs);
        }
        return b;
    }
}
