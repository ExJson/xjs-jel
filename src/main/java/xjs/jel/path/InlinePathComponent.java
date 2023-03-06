package xjs.jel.path;

import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.scope.ReferenceAccessor;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.ContainerToken;

import java.util.Collections;
import java.util.List;

public class InlinePathComponent extends PathComponent {

    public InlinePathComponent(final ContainerToken source, final Sequence<?> sub) {
        super(sub.type(), source, buildList(sub));
    }

    @Override
    public List<JsonReference> getAll(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) throws JelException {
        final JsonValue value = ctx.eval((Sequence<?>)this.subs.get(0));
        return Collections.singletonList(new JsonReference(value));
    }
}
