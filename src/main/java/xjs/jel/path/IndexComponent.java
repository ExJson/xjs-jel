package xjs.jel.path;

import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.scope.ReferenceAccessor;
import xjs.jel.sequence.JelType;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.NumberToken;

import java.util.ArrayList;
import java.util.List;

public class IndexComponent extends PathComponent {
    public final int index;

    public IndexComponent(final ContainerToken source, final NumberToken index) {
        super(JelType.INDEX, source, new ArrayList<>(source.viewTokens()));
        this.index = (int) index.number;
    }

    @Override
    public List<JsonReference> getAll(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) {
        final int idx = wrapIndex(accessor.localSize(), this.index);
        return buildList(accessor.get(idx));
    }
}
