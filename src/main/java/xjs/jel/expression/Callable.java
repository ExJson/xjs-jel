package xjs.jel.expression;

import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.scope.Scope;
import xjs.serialization.Span;

@FunctionalInterface
public interface Callable extends Expression {

    // todo: callable can return callable facade if needed now, removing need to eval expression ots
    Expression call(
        final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException;

    @Override
    default JsonValue apply(final JelContext ctx) throws JelException {
        JelException e = new JelException("check instanceof Callable");
        if (this instanceof Span<?>) {
            e = e.withSpan((Span<?>) this);
        }
        throw e;
    }

    default boolean capturesScope() {
        return false;
    }

    default void setCapture(final Scope scope) {}

    default boolean hasCapture() {
        return false;
    }
}
