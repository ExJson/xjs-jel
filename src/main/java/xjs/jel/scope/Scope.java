package xjs.jel.scope;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonArray;
import xjs.core.JsonReference;
import xjs.jel.lang.JelObject;
import xjs.jel.expression.Callable;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public final class Scope implements ReferenceAccessor {
    private final @Nullable String filePath;
    private final Stack<JsonArray> indexStack;
    private final JelObject map;
    private final Stack<Frame> frames;
    private JsonArray byIndex;
    private Frame frame;

    public Scope() {
        this(null);
    }

    public Scope(final String filePath) {
        this(
            filePath,
            new Stack<>(),
            new JelObject(),
            new Stack<>(),
            new Frame(),
            new JsonArray());
    }

    private Scope(
            final @Nullable String filePath,
            final Stack<JsonArray> indexStack,
            final JelObject map,
            final Stack<Frame> frames,
            final Frame frame,
            final JsonArray byIndex) {
        this.filePath = filePath;
        this.indexStack = indexStack;
        this.map = map;
        this.frames = frames;
        this.byIndex = byIndex;
        this.frame = frame;
    }

    public @Nullable String getFilePath() {
        return this.filePath;
    }

    public void pushFrame() {
        this.indexStack.add(this.byIndex);
        this.frame = new Frame();
        this.frames.add(this.frame);
        this.byIndex = new JsonArray();
    }

    public void add(final String key, final JsonReference ref) {
        this.map.addReference(key, ref);
        this.frame.references.add(key);
        this.byIndex.addReference(ref);
    }

    public void addCallable(final String key, final Callable callable) {
        if (callable.capturesScope() && !callable.hasCapture()) {
            callable.setCapture(this.capture());
        }
        this.map.addCallable(key, callable);
        this.frame.callables.add(key);
    }

    @Override
    public JsonReference get(final String key) {
        return this.map.getDeclaredReference(key);
    }

    @Override
    public Callable getCallable(final String key) {
        return this.map.getCallable(key);
    }

    public void add(final JsonReference ref) {
        this.byIndex.addReference(ref);
    }

    @Override
    public JsonReference get(final int index) {
        if (index < 0 || index >= this.byIndex.size()) {
            return null;
        }
        return this.byIndex.getReference(index);
    }

    public void dropFrame() {
        this.byIndex = this.indexStack.pop();
        final Frame dropped = this.frames.pop();
        dropped.references.forEach(this.map::remove);
        dropped.callables.forEach(this.map::removeCallable);
    }

    @Override
    public int localSize() {
        return this.byIndex.size();
    }

    public Scope capture() {
        return new Scope(
            this.filePath,
            this.copyIndexStack(),
            this.copyMap(),
            this.copyFrames(),
            this.copyFrame(),
            this.copyByIndex());
    }

    public void dispose() {
        this.indexStack.clear();
        this.map.clear();
        this.frames.clear();
        this.byIndex.clear();
        this.frame.callables.clear();
        this.frame.references.clear();
    }

    // ignoring deep copies for now
    private Stack<JsonArray> copyIndexStack() {
        final Stack<JsonArray> out = new Stack<>();
        out.addAll(this.indexStack);
        return out;
    }

    private JelObject copyMap() {
        return (JelObject) this.map.shallowCopy();
    }

    private Stack<Frame> copyFrames() {
        final Stack<Frame> copy = new Stack<>();
        this.frames.forEach(frame -> copy.add(frame.copy()));
        return copy;
    }

    private Frame copyFrame() {
        return this.frame.copy();
    }

    private JsonArray copyByIndex() {
        return new JsonArray().addAll(this.byIndex);
    }

    private static class Frame {
        final List<String> references = new ArrayList<>();
        final List<String> callables = new ArrayList<>();

        Frame copy() {
            final Frame copy = new Frame();
            copy.references.addAll(this.references);
            copy.callables.addAll(this.callables);
            return copy;
        }
    }
}
