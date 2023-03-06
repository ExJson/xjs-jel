package xjs.jel.scope;

import xjs.core.JsonReference;
import xjs.jel.expression.Callable;

public interface ReferenceAccessor {
    JsonReference get(final String key);
    JsonReference get(final int index);
    Callable getCallable(final String key);
    int localSize();

    default boolean isEmpty() {
        return this.localSize() == 0;
    }
}
