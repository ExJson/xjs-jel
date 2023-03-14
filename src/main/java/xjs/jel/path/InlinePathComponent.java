package xjs.jel.path;

import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
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
        final JsonValue value = ((Expression) this.subs.get(0)).apply(ctx);
        return Collections.singletonList(new JsonReference(value));
    }
}
