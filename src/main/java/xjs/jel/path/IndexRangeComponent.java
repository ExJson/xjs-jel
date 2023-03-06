package xjs.jel.path;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.scope.ReferenceAccessor;
import xjs.jel.sequence.JelType;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.NumberToken;

import java.util.ArrayList;
import java.util.List;

public class IndexRangeComponent extends PathComponent {
    public final int s;
    public final int e;

    public IndexRangeComponent(
            final ContainerToken source,
            final @Nullable NumberToken s,
            final @Nullable NumberToken e) {
        super(JelType.INDEX_RANGE, source, new ArrayList<>(source.viewTokens()));
        this.s = s != null ? (int) s.number : 0;
        this.e = e != null ? (int) e.number : -1;
    }

    @Override
    public List<JsonReference> getAll(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) {
        final int size = accessor.localSize();
        int si = wrapIndex(size, this.s);
        int ei = wrapIndex(size, this.e);
        if (si > ei) {
            final int tmp = si;
            si = ei;
            ei = tmp;
        }
        si = Math.max(0, si);
        ei = Math.min(size - 1, ei);
        final List<JsonReference> refs = new ArrayList<>();
        for (int i = si; i <= ei; i++) {
            refs.add(accessor.get(i));
        }
        return refs;
    }
}
