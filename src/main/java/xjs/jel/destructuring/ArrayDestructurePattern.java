package xjs.jel.destructuring;

import xjs.core.JsonContainer;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelFlags;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;

import java.util.List;

public class ArrayDestructurePattern extends DestructurePattern {
    public final List<Span<?>> beginning;
    public final List<Span<?>> end;

    public ArrayDestructurePattern(
            final ContainerToken source, final List<Span<?>> beginning, final List<Span<?>> end) {
        super(JelType.ARRAY_PATTERN, source);
        this.beginning = beginning;
        this.end = end;
    }

    @Override
    public void destructure(
            final JsonContainer from, final JsonObject into) throws JelException {
        for (int i = 0; i < this.beginning.size(); i++) {
            if (i >= from.size()) {
                break;
            }
            this.copy(from, into, i, this.beginning.get(i));
        }
        final int s = Math.max(0, this.end.size() - from.size());
        for (int i = s; i < this.end.size(); i++) {
            final int inv = this.end.size() - i;
            final int idx = wrapIndex(from.size(), from.size() - inv);
            if (idx < 0 || idx >= from.size()) {
                break;
            }
            this.copy(from, into, idx, this.end.get(i));
        }
    }

    protected void copy(
            final JsonContainer from, final JsonObject into,
            final int idx, final Span<?> span) throws JelException {
        final JsonReference ref = from.getReference(idx);
        if (ref == null) {
            return;
        }
        this.checkImport(ref, idx, span, from);
        if (span instanceof ParsedToken) {
            into.addReference(((ParsedToken) span).parsed(), ref);
        } else if (span instanceof DestructurePattern) {
            final DestructurePattern pattern = (DestructurePattern) span;
            final JsonValue v = ref.getOnly();
            if (v.isPrimitive()) {
                throw this.error("cannot destructure element as container", span, v);
            }
            pattern.destructure(v.asContainer(), into);
        } else {
            throw new IllegalStateException("unsupported span: " + span);
        }
    }

    protected void checkImport(
            final JsonReference ref, final int i,
            final Span<?> span, final JsonContainer from) throws JelException {
        if (ref.getOnly().hasFlag(JelFlags.PRIVATE)) {
            throw this.error(
                "element has private access: [" + i + "]=" + ref.getOnly(), span, from);
        }
    }

    protected static int wrapIndex(final int size, final int idx) {
        return idx >= 0 ? idx : size + idx;
    }
}
