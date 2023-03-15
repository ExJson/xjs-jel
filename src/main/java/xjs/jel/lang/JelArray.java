package xjs.jel.lang;

import xjs.core.JsonArray;
import xjs.core.JsonCopy;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.expression.Callable;

import java.util.ArrayList;
import java.util.List;

public class JelArray extends JsonArray implements JelContainer {
    private final List<JsonReference> declared;

    public JelArray() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    private JelArray(
            final List<JsonReference> references,
            final List<JsonReference> declared) {
        super(references);
        this.declared = declared;
    }

    @Override
    public JelArray asArray() {
        return this;
    }

    @Override
    public JelObject asObject() {
        return JelContainer.super.asObject();
    }

    public JelArray addCallable(final Callable callable) {
        this.declared.add(new JsonReference(new CallableFacade(callable)));
        return this;
    }

    @Override
    public JelArray addReference(final JsonReference reference) {
        if (JelContainer.isVisible(reference.getOnly())) {
            super.addReference(reference);
        }
        this.declared.add(reference);
        return this;
    }

    @Override
    public JsonReference getDeclaredReference(final int idx) {
        return this.declared.get(idx);
    }

    @Override
    public JsonValue getDeclared(final int idx) {
        return this.getDeclaredReference(idx).get();
    }

    public JelArray removeDeclared(final int idx) {
        final JsonReference removed = this.declared.remove(idx);
        final int visibleIdx =
            JelContainer.indexOfExactly(this.references, removed);
        if (visibleIdx != -1) {
            super.remove(visibleIdx);
        }
        return this;
    }

    @Override
    public JelArray remove(final int idx) {
        final JsonReference removed = this.references.remove(idx);
        if (removed != null) {
            final int declaredIdx =
                JelContainer.indexOfExactly(this.declared, removed);
            if (declaredIdx != -1) {
                this.declared.remove(declaredIdx);
            }
        }
        return this;
    }

    @Override
    public JsonArray getDeclaredValues() {
        return new JsonArray(this.declared);
    }

    @Override
    public int declaredSize() {
        return this.declared.size();
    }

    @Override
    public JelArray copy(final int options) {
        final List<JsonReference> declaredCopy = new ArrayList<>(this.declared);
        final List<JsonReference> visible = new ArrayList<>();

        for (final JsonReference reference : declaredCopy) {
            if (JelContainer.isVisible(reference.getOnly())) {
                visible.add(reference);
            }
        }
        final JelArray copy = new JelArray(visible, declaredCopy);
        if ((options & JsonCopy.FORMATTING) == JsonCopy.FORMATTING) {
            copy.setLinesTrailing(this.linesTrailing);
        }
        return withMetadata(copy, this, options);
    }
}
