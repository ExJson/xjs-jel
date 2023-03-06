package xjs.jel.performance.experimental;

import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;

import java.util.List;

public class NewArithmeticExpression extends NewOperatorExpression {

    public NewArithmeticExpression(final List<Span<?>> subs) {
        super(JelType.NUMBER_EXPRESSION, subs);
    }

    @Override
    protected JsonValue calculate(
            final JelContext ctx, final Sequence<Span<?>>.Itr itr,
            final JsonValue first) throws JelException {
        return null;
    }

    public static class Add extends ArithmeticClause {

        protected Add(final Span<?> operand) {
            super(operand);
        }

        @Override
        public double applyAsDouble(final JelContext ctx, final double lhs) {
            return 0;
        }
    }
}
