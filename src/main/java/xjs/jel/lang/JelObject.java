package xjs.jel.lang;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonCopy;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelMember;
import xjs.jel.expression.Callable;
import xjs.serialization.util.HashIndexTable;

import java.util.ArrayList;
import java.util.List;

// still investigating optimizations
public class JelObject extends JsonObject implements JelContainer {
    private final List<String> callableKeys;
    private final List<Callable> callables;
    private final transient HashIndexTable callableTable;
    private final List<String> declaredKeys;
    private final List<JsonReference> declared;
    private final transient HashIndexTable declaredTable;

    public JelObject() {
        this(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>());
    }

    private JelObject(
            final List<String> keys,
            final List<JsonReference> references,
            final List<String> callableKeys,
            final List<Callable> callables,
            final List<String> declaredKeys,
            final List<JsonReference> declared) {
        super(keys, references);
        this.callableKeys = callableKeys;
        this.callables = callables;
        this.callableTable = new HashIndexTable();
        this.callableTable.init(this.callableKeys);
        this.declaredKeys = declaredKeys;
        this.declared = declared;
        this.declaredTable = new HashIndexTable();
        this.declaredTable.init(this.declaredKeys);
    }

    @Override
    public JelArray asArray() {
        return JelContainer.super.asArray();
    }

    @Override
    public JelObject asObject() {
        return this;
    }

    public JelObject addCallable(final String key, final Callable callable) {
        this.callableTable.add(key, this.callableKeys.size());
        this.callableKeys.add(key);
        this.callables.add(callable);
        return this;
    }

    public Callable getCallable(final String key) {
        int index = this.indexOfCallable(key);
        if (index != -1) {
            return this.callables.get(index);
        }
        index = this.indexOfDeclared(key);
        if (index != -1) {
            final JsonReference ref = this.declared.get(index);
            if (ref.getOnly() instanceof CallableFacade) {
                return ((CallableFacade) ref.get()).getWrapped();
            }
        }
        return null;
    }

    public int indexOfCallable(final String key) {
        int index = this.callableTable.get(key);
        return index != -1 && key.equals(this.callableKeys.get(index))
            ? index : this.callableKeys.lastIndexOf(key);
    }

    @Override
    public JsonObject addReference(final String key, final JsonReference reference) {
        if (JelContainer.isVisible(reference.getOnly())) {
            super.addReference(key, reference);
        }
        this.declaredTable.add(key, this.declaredKeys.size());
        this.declaredKeys.add(key);
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

    public @Nullable JsonReference getDeclaredReference(final String key) {
        // in the new system, values always take precedent over callables,
        // so there's no need to store them separately and do a double lookup

        final int index = this.indexOfDeclared(key);
        if (index != -1) {
            return this.declared.get(index);
        }
        final Callable callable = getCallable(key);
        if (callable != null) {
            return new JsonReference(new CallableFacade(callable));
        }
        return null;
    }

    public @Nullable JsonValue getDeclared(final String key) {
        final JsonReference ref = this.getDeclaredReference(key);
        return ref != null ? ref.get() : null;
    }

    public int indexOfDeclared(final String key) {
        int index = this.declaredTable.get(key);
        return index != -1 && key.equals(this.declaredKeys.get(index))
            ? index : this.declaredKeys.lastIndexOf(key);
    }

    public JelObject removeDeclared(final String key) {
        int index = this.indexOfDeclared(key);
        if (index == -1) {
            return this;
        }
        final JsonValue removed =
            this.declared.remove(index).getOnly();
        this.declaredKeys.remove(index);
        this.declaredTable.remove(index);
        // premature optimization?
        if (!JelContainer.isVisible(removed)) {
            super.remove(key);
        }
        return this;
    }

    @Override
    public JelObject remove(final String key) {
        return this.removeDeclared(key);
    }

    public JelObject removeCallable(final String key) {
        final int index = this.indexOfCallable(key);
        if (index >= 0) {
            this.callableKeys.remove(index);
            this.callables.remove(index);
            this.callableTable.remove(index);
        }
        return this;
    }

    public List<JelMember> jelMembers() {
        final int len = this.callables.size() + this.declared.size();
        final List<JelMember> members = new ArrayList<>(len);
        for (int i = 0; i < this.callables.size(); i++) {
            members.add(JelMember.of(this.callableKeys.get(i), this.callables.get(i)));
        }
        for (int i = 0; i < this.declared.size(); i++) {
            members.add(JelMember.of(this.declaredKeys.get(i), this.declared.get(i).getOnly()));
        }
        return members;
    }

    @Override
    public JsonObject getDeclaredValues() {
        final JsonObject declared = new JsonObject();
        for (int i = 0; i < this.declared.size(); i++) {
            declared.addReference(this.declaredKeys.get(i), this.declared.get(i));
        }
        for (int i = 0; i < this.callables.size(); i++) {
            final Callable callable = this.callables.get(i);
            declared.add(this.callableKeys.get(i), new CallableFacade(callable));
        }
        return declared;
    }

    @Override
    public int declaredSize() {
        return this.declared.size();
    }

    @Override
    public JelObject copy(final int options) {
        final List<String> declaredKeysCopy = new ArrayList<>(this.declaredKeys);
        final List<JsonReference> declaredCopy = new ArrayList<>(this.declared);
        final List<String> visibleKeys = new ArrayList<>();
        final List<JsonReference> visible = new ArrayList<>();

        for (int i = 0; i < declaredCopy.size(); i++) {
            final JsonReference reference = declaredCopy.get(i);
            if (JelContainer.isVisible(reference.getOnly())) {
                visibleKeys.add(declaredKeysCopy.get(i));
                visible.add(reference);
            }
        }
        final JelObject copy = new JelObject(
            visibleKeys, visible, new ArrayList<>(this.callableKeys),
            new ArrayList<>(this.callables), declaredKeysCopy, declaredCopy);
        if ((options & JsonCopy.FORMATTING) == JsonCopy.FORMATTING) {
            copy.setLinesTrailing(this.linesTrailing);
        }
        return withMetadata(copy, this, options);
    }
}
