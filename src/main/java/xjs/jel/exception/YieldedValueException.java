package xjs.jel.exception;

import xjs.core.JsonValue;

public class YieldedValueException extends RuntimeException {
    private final JsonValue value;

    public YieldedValueException(final JsonValue value) {
        this.value = value;
    }

    public JsonValue getValue() {
        return this.value;
    }
}
