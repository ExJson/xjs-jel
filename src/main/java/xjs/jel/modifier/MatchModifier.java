package xjs.jel.modifier;

import xjs.jel.expression.ReferenceExpression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.Token;

public class MatchModifier
        extends Sequence.Combined implements Modifier {
    public final ReferenceExpression ref;

    public MatchModifier(final Token token, final ReferenceExpression ref) {
        super(JelType.FLAG, buildList(token, ref));
        this.ref = ref;
    }

    @Override
    public JelType getValueType() {
        return JelType.MATCH;
    }
}
