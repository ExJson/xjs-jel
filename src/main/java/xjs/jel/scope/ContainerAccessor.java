package xjs.jel.scope;

import xjs.core.JsonContainer;
import xjs.core.JsonReference;
import xjs.jel.expression.Callable;
import xjs.jel.lang.JelReflection;

public class ContainerAccessor implements ReferenceAccessor {
    private final JsonContainer container;

    public ContainerAccessor(final JsonContainer container) {
        this.container = container;
    }

    @Override
    public JsonReference get(final String key) {
        if (this.container.isObject()) {
            return JelReflection.getReference(this.container.asObject(), key);
        }
        return null;
    }

    @Override
    public JsonReference get(final int index) {
        if (index < 0) {
            return null;
        }
        final int size = JelReflection.getSize(this.container);
        if (index >= size) {
            return null;
        }
        return JelReflection.getReference(this.container, index);
    }

    @Override
    public Callable getCallable(final String key) {
        return this.container.isObject()
            ? JelReflection.getCallable(this.container.asObject(), key) : null;
    }

    @Override
    public int localSize() {
        return JelReflection.getSize(this.container);
    }
}
