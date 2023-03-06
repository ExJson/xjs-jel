package xjs.jel.expression;

import xjs.jel.JelContext;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;

import java.util.Collections;
import java.util.List;

public class DefaultCaseExpression extends BooleanExpression {

    public DefaultCaseExpression(final List<Sequence<?>> subs) {
        super(JelType.SYMBOL, subs);
    }

    @Override
    public boolean applyAsBoolean(final JelContext ctx){
        return true;
    }

    @Override
    public List<Span<?>> flatten() {
        return Collections.singletonList(this);
    }
}
