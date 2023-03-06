package xjs.jel.path;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.expression.Callable;
import xjs.jel.lang.JelFunctions;
import xjs.jel.scope.ReferenceAccessor;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ParsedToken;

import java.util.Collections;
import java.util.List;

public class KeyComponent extends PathComponent {
    public final String key;

    public KeyComponent(final ParsedToken token) {
        super(JelType.KEY, buildList(token));
        this.key = token.parsed();
    }

    @Override
    public List<JsonReference> getAll(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) {
        return buildList(accessor.get(this.key));
    }

    @Override
    public List<Span<?>> flatten() {
        return Collections.singletonList(this);
    }

    @Override
    public @Nullable Callable getCallable(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) {
        Callable c = accessor != null
            ? accessor.getCallable(this.key) : null;
        if (c != null) {
            return c;
        }
        if (accessor != ctx.getScope()) {
            c = ctx.getScope().getCallable(this.key);
            if (c != null) {
                return c;
            }
        }
        return JelFunctions.lookup(ctx.getPrivilege(), this.key);
    }
}
