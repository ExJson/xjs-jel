package xjs.jel.exception;

import org.jetbrains.annotations.NotNull;
import xjs.core.JsonValue;
import xjs.serialization.Span;

public class YieldException extends JumpException {
    private final JsonValue value;

    public YieldException(final Span<?> span, final JsonValue value) {
        super(span);
        this.value = value;
    }

    @Override
    public @NotNull JsonValue getValue() {
        return this.value;
    }
}
