package xjs.jel.modifier;

import xjs.jel.expression.ArrayGeneratorExpression;
import xjs.jel.expression.Expression;
import xjs.jel.expression.TupleExpression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;

public class ArrayGeneratorModifier
        extends Sequence.Parent implements Modifier {

    public ArrayGeneratorModifier(final TupleExpression tuple) {
        super(JelType.ARRAY_GENERATOR, buildList(tuple));
    }

    @Override
    public Expression modify(final Expression expression) {
        return new ArrayGeneratorExpression(
            (TupleExpression) this.subs.get(0), expression);
    }
}
