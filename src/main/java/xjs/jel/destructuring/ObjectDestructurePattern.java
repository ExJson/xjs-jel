package xjs.jel.destructuring;

import xjs.core.JsonContainer;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.jel.JelFlags;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.lang.JelObject;
import xjs.jel.sequence.JelType;
import xjs.serialization.token.ContainerToken;

import java.util.List;

public class ObjectDestructurePattern extends DestructurePattern {
    public final List<KeyPattern> keys;

    public ObjectDestructurePattern(
            final ContainerToken source, final List<KeyPattern> keys) {
        super(JelType.OBJECT_PATTERN, source);
        this.keys = keys;
    }

    @Override
    public void destructure(final JsonContainer from, final JsonObject into) throws JelException {
        if (from.isArray()) {
            throw this.error("Cannot destructure array as object", from);
        }
        for (final KeyPattern key : this.keys) {
            final JsonReference ref = from.asObject().getReference(key.source);
            if (ref != null) {
                this.checkImport(ref, key, from);
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
            final JsonReference ref, final KeyPattern pattern, final JsonContainer from) throws JelException {
        if (ref.getOnly().hasFlag(JelFlags.PRIVATE)) {
            throw this.error(
                "Key has private access: " + pattern.source, pattern, from);
        }
    }
}
