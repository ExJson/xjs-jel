package xjs.jel.util;

import org.jetbrains.annotations.Nullable;
import xjs.serialization.Span;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SpanMap extends HashMap<String, List<Span<?>>> {

    public void add(final @Nullable String path, final Iterable<Span<?>> toAdd) {
        final List<Span<?>> spans = this.getOrCreate(path);
        toAdd.forEach(spans::add);
    }

    public void add(final @Nullable String path, final Span<?>... toAdd) {
        for (Span<?> span : toAdd) {
            Objects.requireNonNull(span, "span");
        }
        Collections.addAll(this.getOrCreate(path), toAdd);
    }

    protected List<Span<?>> getOrCreate(final @Nullable String path) {
        return this.computeIfAbsent(path, s -> new ArrayList<>());
    }
}
