package xjs.jel.lang;

import xjs.core.JsonCopy;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.jel.JelMember;
import xjs.jel.expression.Callable;
import xjs.serialization.util.HashIndexTable;

import java.util.ArrayList;
import java.util.List;

public class JelObject extends JsonObject {
    private final List<String> callableKeys;
    private final List<Callable> callables;
    private final transient HashIndexTable callableTable;

    public JelObject() {
        this(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>());
    }

    private JelObject(
            final List<String> keys,
            final List<JsonReference> references,
            final List<String> callableKeys,
            final List<Callable> callables) {
        super(keys, references);
        this.callableKeys = callableKeys;
        this.callables = callables;
        this.callableTable = new HashIndexTable();
        this.callableTable.init(this.callableKeys);
    }

    public JelObject addCallable(final String key, final Callable callable) {
        this.callableTable.add(key, this.callableKeys.size());
        this.callableKeys.add(key);
        this.callables.add(callable);
        return this;
    }

    public Callable getCallable(final String key) {
        final int index = this.indexOfCallable(key);
        return index != -1 ? this.callables.get(index) : null;
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

    public int indexOfCallable(final String key) {
        int index = this.callableTable.get(key);
        return index != -1 && key.equals(this.callableKeys.get(index))
            ? index : this.callableKeys.lastIndexOf(key);
    }

    public List<JelMember> getCallables() {
        final List<JelMember> members = new ArrayList<>(this.callables.size());
        for (int i = 0; i < this.callables.size(); i++) {
            members.add(JelMember.of(this.callableKeys.get(i), this.callables.get(i)));
        }
        return members;
    }

    @Override
    public JelObject copy(final int options) {
        JelObject copy = new JelObject(
            new ArrayList<>(this.keys()), this.copyReferences(options),
            new ArrayList<>(this.callableKeys), new ArrayList<>(this.callables));
        if ((options & JsonCopy.FORMATTING) == JsonCopy.FORMATTING) {
            copy.setLinesTrailing(this.linesTrailing);
        }
        return withMetadata(copy, this, options);
    }
}
