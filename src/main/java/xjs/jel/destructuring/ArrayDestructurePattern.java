package xjs.jel.destructuring;

import xjs.core.JsonContainer;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelFlags;
import xjs.jel.exception.JelException;
import xjs.jel.lang.JelReflection;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.Collections;
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
            final JelContext ctx, final JsonContainer from, final JsonObject into) throws JelException {
        for (int i = 0; i < this.beginning.size(); i++) {
            if (i >= from.size()) {
                break;
            }
            this.copy(ctx, from, into, i, this.beginning.get(i));
        }
        final int len = JelReflection.getSize(from);
        final int s = Math.max(0, this.end.size() - len);
        for (int i = s; i < this.end.size(); i++) {
            final int inv = this.end.size() - i;
            final int idx = wrapIndex(from.size(), len - inv);
            if (idx < 0 || idx >= from.size()) {
                break;
            }
            this.copy(ctx, from, into, idx, this.end.get(i));
        }
    }

    protected void copy(
            final JelContext ctx, final JsonContainer from, final JsonObject into,
            final int idx, final Span<?> span) throws JelException {
        final JsonReference ref = JelReflection.getReference(from, idx);
        if (ref == null) {
            return;
        }
        if (span instanceof ParsedToken) {
            into.addReference(((ParsedToken) span).parsed(), ref);
        } else if (span instanceof DestructurePattern) {
            final DestructurePattern pattern = (DestructurePattern) span;
            final JsonValue v = ref.getOnly();
            if (v.isPrimitive()) {
                throw this.error(ctx, "Cannot destructure element as container", span, v);
            }
            pattern.destructure(ctx, v.asContainer(), into);
        } else {
            throw new IllegalStateException("unsupported span: " + span);
        }
    }

    protected static int wrapIndex(final int size, final int idx) {
        return idx >= 0 ? idx : size + idx;
    }

    @Override
    public List<Span<?>> flatten() {
        final List<Span<?>> flat = new ArrayList<>();
        this.flattenInto(flat, this.beginning);
        this.flattenInto(flat, this.end);
        return flat;
    }

    protected void flattenInto(final List<Span<?>> flat, final List<Span<?>> source) {
        for (final Span<?> sub : source) {
            if (sub instanceof ParsedToken) {
                flat.add(new Sequence.Primitive(
                    JelType.KEY, Collections.singletonList((Token) sub)));
            } else if (sub instanceof Sequence<?>) {
                flat.addAll(((Sequence<?>) sub).flatten());
            } else {
                flat.add(sub);
            }
        }
    }
}
