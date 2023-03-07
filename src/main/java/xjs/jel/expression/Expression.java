package xjs.jel.expression;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonType;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.modifier.Modifier;

import java.util.List;

@FunctionalInterface
public interface Expression {

    JsonValue apply(final JelContext ctx) throws JelException;

    default double applyAsNumber(final JelContext ctx) throws JelException {
        return this.apply(ctx).intoDouble();
    }

    default String applyAsString(final JelContext ctx) throws JelException {
        return this.apply(ctx).intoString();
    }

    default boolean applyAsBoolean(final JelContext ctx) throws JelException {
        return this.apply(ctx).intoBoolean();
    }

    default @Nullable JsonType getStrongType() {
        return null;
    }

    default boolean isInlined() {
        return false;
    }
}
