package xjs.jel.performance.experimental;

import xjs.core.Json;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.ParsedToken;

import java.util.List;

public abstract class NewOperatorExpression
        extends Sequence.Combined implements Expression {

    public NewOperatorExpression(final JelType type, final List<Span<?>> subs) {
        super(type, subs);
    }

    @Override
    public JsonValue apply(final JelContext ctx) throws JelException {
        final Sequence<Span<?>>.Itr itr = this.iterator();
        if (!itr.hasNext()) {
            throw new IllegalStateException("empty expression was not caught");
        }
        return this.calculate(ctx, itr, valueOf(ctx, itr.next()));
    }

    protected abstract JsonValue calculate(
            final JelContext ctx, final Sequence<Span<?>>.Itr itr,
            final JsonValue first) throws JelException;

    protected static JsonValue valueOf(
            final JelContext ctx, final Span<?> span) throws JelException {
        if (span instanceof Expression) {
            return ((Expression) span).apply(ctx);
        } else if (span instanceof ParsedToken) {
            final String parsed = ((ParsedToken) span).parsed();
            switch (parsed) {
                case "true": return Json.value(true);
                case "false": return Json.value(false);
                case "null": return Json.value(null);
                default: return Json.value(parsed);
            }
        } else if (span instanceof NumberToken) {
            return Json.value(((NumberToken) span).number);
        } else if (span instanceof Sequence<?>) {
            return ctx.eval((Sequence<?>) span);
        }
        throw new IllegalStateException("Unsupported type: " + span.getClass());
    }

    protected static double doubleOf(
            final JelContext ctx, final Span<?> span) throws JelException {
        throw new UnsupportedOperationException("todo");
    }

    protected static boolean booleanOf(
            final JelContext ctx, final Span<?> span) throws JelException {
        throw new UnsupportedOperationException("todo");
    }

    public static abstract class Clause extends Sequence.Combined {
        protected Clause(final JelType type, final Span<?> operand) {
            super(type, buildList(operand));
        }

        public abstract JsonValue apply(
                final JelContext ctx, final JsonValue lhs) throws JelException;

        protected final JsonValue getRhs(final JelContext ctx) throws JelException {
            return valueOf(ctx, this.subs.get(0));
        }
    }

    public static abstract class ArithmeticClause extends Clause {
        protected ArithmeticClause(final Span<?> operand) {
            super(JelType.NUMBER_EXPRESSION, operand);
        }

        public abstract double applyAsDouble(
                final JelContext ctx, final double lhs) throws JelException;

        @Override
        public JsonValue apply(
                final JelContext ctx, final JsonValue lhs) throws JelException {
            return Json.value(this.applyAsDouble(ctx, lhs.intoDouble()));
        }

        protected final double getDouble(final JelContext ctx) throws JelException {
            return doubleOf(ctx, this.subs.get(0));
        }
    }

    public static abstract class BooleanClause extends Clause {
        protected BooleanClause(final Span<?> operand) {
            super(JelType.BOOLEAN_EXPRESSION, operand);
        }

        public abstract boolean applyAsBoolean(
                final JelContext ctx, final boolean lhs) throws JelException;

        @Override
        public JsonValue apply(
                final JelContext ctx, final JsonValue lhs) throws JelException {
            return Json.value(this.applyAsBoolean(ctx, lhs.intoBoolean()));
        }

        protected final boolean getBoolean(final JelContext ctx) throws JelException {
            return booleanOf(ctx, this.subs.get(0));
        }
    }

    public static abstract class RelationalClause extends Clause {
        protected RelationalClause(final Span<?> operand) {
            super(JelType.NUMBER_EXPRESSION, operand);
        }

        public abstract boolean compareTo(
                final JelContext ctx, final double lhs) throws JelException;

        @Override
        public JsonValue apply(
                final JelContext ctx, final JsonValue lhs) throws JelException {
            return Json.value(this.compareTo(ctx, lhs.intoDouble()));
        }

        protected final double getDouble(final JelContext ctx) throws JelException {
            return doubleOf(ctx, this.subs.get(0));
        }
    }
}
