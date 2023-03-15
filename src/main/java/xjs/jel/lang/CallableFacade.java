package xjs.jel.lang;

import xjs.core.JsonType;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.expression.Expression;

public class CallableFacade extends JsonValue implements Callable {
    private final Callable wrapped;

    public CallableFacade(final Callable wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Expression call(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return this.wrapped.call(self, ctx, args);
    }

    public Callable getWrapped() {
        return this.wrapped;
    }

    @Override
    public JsonType getType() {
        return JsonType.NULL;
    }

    @Override
    public Object unwrap() {
        throw new UnsupportedOperationException("check instanceof Callable");
    }

    @Override
    public double intoDouble() {
        throw new UnsupportedOperationException("check instanceof Callable");
    }

    @Override
    public JsonValue copy(final int options) {
        return withMetadata(new CallableFacade(this.wrapped), this, options);
    }
}
