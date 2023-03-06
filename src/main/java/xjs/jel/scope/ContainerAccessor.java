package xjs.jel.scope;

import xjs.core.JsonContainer;
import xjs.core.JsonReference;
import xjs.jel.lang.JelObject;
import xjs.jel.expression.Callable;

public class ContainerAccessor implements ReferenceAccessor {
    private final JsonContainer container;

    public ContainerAccessor(final JsonContainer container) {
        this.container = container;
    }

    @Override
    public JsonReference get(final String key) {
        if (this.container.isObject()) {
            return this.container.asObject().getReference(key);
        }
        return null;
    }

    @Override
    public JsonReference get(final int index) {
        if (index < 0 || index >= this.container.size()) {
            return null;
        }
        return this.container.getReference(index);
    }

    @Override
    public Callable getCallable(final String key) {
        if (this.container instanceof JelObject) {
            return ((JelObject) this.container).getCallable(key);
        }
        return null;
    }

    @Override
    public int localSize() {
        return this.container.size();
    }
}
