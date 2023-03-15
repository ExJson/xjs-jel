package xjs.jel.lang;

import xjs.core.JsonContainer;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelFlags;
import xjs.jel.expression.Callable;

import java.util.Collection;

public interface JelContainer {

    JsonContainer getDeclaredValues();

    default JelObject asObject() {
        throw new UnsupportedOperationException("not an object");
    }

    default JelArray asArray() {
        throw new UnsupportedOperationException("not an array");
    }

    JsonReference getDeclaredReference(final int idx);

    JsonValue getDeclared(final int idx);

    int declaredSize();

    static boolean isVisible(final JsonValue value) {
        return !(value instanceof Callable)
            && !value.hasFlag(JelFlags.VAR)
            && !value.hasFlag(JelFlags.PRIVATE);
    }

    static int indexOfExactly(
            final Collection<JsonReference> references, final JsonReference value) {
        int i = 0;
        for (final JsonReference reference : references) {
            if (reference == value) {
                return i;
            }
            i++;
        }
        return -1;
    }
}
