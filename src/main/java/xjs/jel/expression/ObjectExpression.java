package xjs.jel.expression;

import xjs.core.JsonReference;
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
            m.getExpression().apply(ctx);
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
        final JsonReference ref = this.getReference(m, ctx);
        if (!ref.getOnly().hasFlag(JelFlags.PRIVATE)) {
            o.addReference(key, ref);
        }
        ctx.getScope().add(m.getKey(), ref);
    }

    protected JsonReference getReference(
            final JelMember m, final JelContext ctx) throws JelException {
        final Expression exp = m.getExpression();
        if (exp instanceof ReferenceExpression && m.hasFlag(JelFlags.VAR)) {
            return ((ReferenceExpression) exp).getReference(ctx);
        }
        return new JsonReference(m.getValue(ctx));
    }
}
