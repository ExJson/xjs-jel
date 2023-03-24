package xjs.jel.exception;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonValue;
import xjs.serialization.Span;

public abstract class JumpException extends RuntimeException {
    private final Span<?> span;

    protected JumpException(final Span<?> span) {
        this.span = span;
    }

    public Span<?> getSpan() {
        return this.span;
    }

    public @Nullable JsonValue getValue() {
        return null;
    }
}
