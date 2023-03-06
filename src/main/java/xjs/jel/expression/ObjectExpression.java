package xjs.jel.expression;

import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelFlags;
import xjs.jel.JelMember;
import xjs.jel.lang.JelObject;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;

import java.util.List;

public class ObjectExpression extends ContainerExpression<JelObject> {

    public ObjectExpression(final ContainerToken source, final List<Span<?>> subs) {
        super(JelType.OBJECT, source, subs);
    }

    @Override
    protected JelObject newContainer() {
        return new JelObject();
    }

    @Override
    protected void addMember(
            final JelContext ctx, final JelObject o, final JelMember m) throws JelException {
        if (m.getAlias() == null) {
            m.getValue(ctx);
            return;
        }
        final String key = m.getKey();
        if (m.getExpression() instanceof Callable) {
            final Callable c = (Callable) m.getExpression();
            if (!m.hasFlag(JelFlags.PRIVATE)) {
                o.addCallable(key, c);
            }
            ctx.getScope().addCallable(key, c);
            return;
        }
        final JsonValue value = m.getValue(ctx);
        final JsonReference ref = new JsonReference(value);
        if (!value.hasFlag(JelFlags.PRIVATE)) {
            o.addReference(key, ref);
        }
        ctx.getScope().add(m.getKey(), ref);
    }
}
