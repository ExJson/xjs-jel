package xjs.jel.modifier;

import xjs.jel.expression.ReferenceExpression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Override
    public List<Span<?>> flatten() {
        final List<Span<?>> flat = new ArrayList<>();
        flat.add(new Sequence.Primitive(
            JelType.FLAG, Collections.singletonList((Token)this.subs.get(0))));
        flat.addAll(((Sequence<?>)this.subs.get(1)).flatten());
        return flat;
    }
}
