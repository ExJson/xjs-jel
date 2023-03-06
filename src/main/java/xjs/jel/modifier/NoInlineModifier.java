package xjs.jel.modifier;

import xjs.jel.JelContext;
import xjs.jel.JelFlags;
import xjs.jel.JelMember;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.Token;

import java.util.Collections;
import java.util.List;

public class NoInlineModifier
        extends Sequence.Primitive implements Modifier {

    public NoInlineModifier(final Token token) {
        super(JelType.FLAG, buildList(token));
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) {
        member.addFlags(JelFlags.NOINLINE);
        return Collections.singletonList(member);
    }

    @Override
    public JelType getValueType() {
        return JelType.STRING;
    }
}
