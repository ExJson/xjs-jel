package xjs.jel.expression;

import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelFlags;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.lang.JelArray;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;

import java.util.List;

public class ArrayExpression extends ContainerExpression<JelArray> {

    public ArrayExpression(final ContainerToken source, final List<Span<?>> subs) {
        super(JelType.ARRAY, source, subs);
    }

    @Override
    protected JelArray newContainer() {
        return new JelArray();
    }

    @Override
    protected void addMember(
            final JelContext ctx, final JelArray a, final JelMember m) throws JelException {
        final JsonValue value = m.getValue(ctx);
        final JsonReference ref = new JsonReference(value);
        if (!value.hasFlag(JelFlags.PRIVATE)) {
            a.addReference(ref);
        }
        ctx.getScope().add(ref);
    }
}
