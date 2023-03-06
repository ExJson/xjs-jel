package xjs.jel.modifier;

import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.expression.Expression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.Token;

import java.util.Collections;
import java.util.List;

public class FlagModifier
        extends Sequence.Primitive implements Modifier {
    private final int flag;

    public FlagModifier(final Token token, final int flag) {
        super(JelType.FLAG, buildList(token));
        this.flag = flag;
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) {
        member.addFlags(this.flag);
        return Collections.singletonList(member);
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> expression.apply(ctx).addFlag(this.flag);
    }
}
