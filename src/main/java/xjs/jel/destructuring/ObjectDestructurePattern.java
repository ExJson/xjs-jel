package xjs.jel.destructuring;

import xjs.core.JsonContainer;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.jel.JelContext;
import xjs.jel.JelFlags;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.lang.JelObject;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;

import java.util.ArrayList;
import java.util.List;

public class ObjectDestructurePattern extends DestructurePattern {
    public final List<KeyPattern> keys;

    public ObjectDestructurePattern(
            final ContainerToken source, final List<KeyPattern> keys) {
        super(JelType.OBJECT_PATTERN, source);
        this.keys = keys;
    }

    @Override
    public void destructure(
            final JelContext ctx, final JsonContainer from, final JsonObject into) throws JelException {
        if (from.isArray()) {
            throw this.error(ctx, "Cannot destructure array as object", from);
        }
        for (final KeyPattern key : this.keys) {
            final JsonReference ref = from.asObject().getReference(key.source);
            if (ref != null) {
                this.checkImport(ctx, ref, key, from);
                into.addReference(key.key, ref);
            } else if (from instanceof JelObject && into instanceof JelObject) {
                final Callable c = ((JelObject) from).getCallable(key.source);
                if (c != null) {
                    ((JelObject) into).addCallable(key.key, c);
                }
            }
        }
    }

    protected void checkImport(
            final JelContext ctx, final JsonReference ref,
            final KeyPattern pattern, final JsonContainer from) throws JelException {
        if (ref.getOnly().hasFlag(JelFlags.PRIVATE)) {
            throw this.error(
                ctx, "Key has private access: " + pattern.source, pattern, from);
        }
    }

    @Override
    public List<Span<?>> flatten() {
        final List<Span<?>> flat = new ArrayList<>();
        for (final KeyPattern key : this.keys) {
            flat.addAll(key.flatten());
        }
        for (final Span<?> sub : this.subs) {
            if (!JelType.isSignificant(sub)) {
                flat.add(sub);
            }
        }
        flat.sort(Span::compareTo);
        return flat;
    }
}
