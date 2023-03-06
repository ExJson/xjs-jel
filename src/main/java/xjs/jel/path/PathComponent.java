package xjs.jel.path;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.scope.ReferenceAccessor;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;

import java.util.List;

public abstract class PathComponent extends Sequence.Combined {
    protected PathComponent(final JelType type, final List<Span<?>> subs) {
        super(type, subs);
    }

    protected PathComponent(final JelType type, final Span<?> source, final List<Span<?>> subs) {
        super(type, source, source, subs);
    }

    public abstract List<JsonReference> getAll(
        final JelContext ctx,
        final ReferenceAccessor accessor,
        final JsonValue parent) throws JelException;

    protected static int wrapIndex(final int size, final int idx) {
        return idx >= 0 ? idx : size + idx;
    }

    protected boolean acceptsNullAccessor() {
        return false;
    }

    public @Nullable Callable getCallable(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) throws JelException {
        return null;
    }
}
