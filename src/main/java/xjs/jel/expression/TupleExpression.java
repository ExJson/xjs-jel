package xjs.jel.expression;

import xjs.core.JsonArray;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;

import java.util.ArrayList;
import java.util.List;

public class TupleExpression
        extends Sequence.Combined implements Expression {
    public final List<Expression> expressions;

    public TupleExpression(final ContainerToken source, final List<Span<?>> subs) {
        super(JelType.TUPLE, source, source, subs);
        this.expressions = filterExpressions(subs);
    }

    private static List<Expression> filterExpressions(final List<Span<?>> subs) {
        final List<Expression> expressions = new ArrayList<>();
        for (final Span<?> sub : subs) {
            if (sub instanceof Expression) {
                expressions.add((Expression) sub);
            }
        }
        return expressions;
    }

    @Override
    public JsonArray apply(final JelContext ctx) throws JelException {
        final JsonArray array = new JsonArray();
        for (final Expression exp : this.expressions) {
            array.add(exp.apply(ctx));
        }
        return array;
    }
}
