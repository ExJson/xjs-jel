package xjs.jel.sequence;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.jel.expression.Expression;
import xjs.serialization.Span;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class Sequence<T extends Span<?>>
        extends Span<JelType>
        implements Iterable<T> {

    protected final List<T> subs;

    protected Sequence(final JelType type, final List<T> subs) {
        super(type);
        this.subs = subs;
        this.init();
    }

    protected Sequence(final JelType type, final Span<?> s, final Span<?> e, final List<T> subs) {
        super(type);
        this.subs = subs;
        this.setFullSpan(s, e);
    }

    protected void init() {
        if (this.subs.isEmpty()) {
            return;
        }
        final Span<?> first = this.subs.get(0);
        this.start = first.start();
        this.offset = first.offset();
        this.line = first.line();

        final Span<?> last = this.subs.get(this.subs.size() - 1);
        this.end = last.end();
        this.lastLine = last.lastLine();
    }

    protected static <T> List<T> buildList(final @Nullable T t) {
        final List<T> list = new ArrayList<>();
        if (t != null) {
            list.add(t);
        }
        return list;
    }

    @SafeVarargs
    protected static <T> List<T> buildList(final T... ts) {
        final List<T> list = new ArrayList<>();
        for (final T t : ts) {
             if (t != null) {
                 list.add(t);
             }
        }
        return list;
    }

    protected static List<Sequence<?>> getSubs(final Expression exp) {
        if (exp instanceof Sequence<?>) {
            return new ArrayList<>(((Sequence<?>) exp).subsequences());
        }
        return Collections.emptyList();
    }

    protected static List<Span<?>> getSpans(final Expression exp) {
        if (exp instanceof Sequence<?>) {
            return new ArrayList<>(((Sequence<?>) exp).spans());
        }
        return Collections.emptyList();
    }

    protected void setFullSpan(final Span<?> s, final Span<?> e) {
        this.start = s.start();
        this.offset = s.offset();
        this.line = s.line();
        this.end = e.end();
        this.lastLine = e.lastLine();
    }

    public List<? extends Span<?>> spans() {
        return this.subs;
    }

    public List<Sequence<?>> subsequences() {
        return Collections.emptyList();
    }

    public List<Token> tokens() {
        return Collections.emptyList();
    }

    public int size() {
        return this.subs.size();
    }

    public abstract boolean isPrimitive();

    public abstract List<Span<?>> flatten();

    public Sequence<Sequence<?>> asParent() {
        throw new UnsupportedOperationException("not a parent");
    }

    public Sequence<Token> asPrimitive() {
        throw new UnsupportedOperationException("not primitive");
    }

    @NotNull
    @Override
    public Itr iterator() {
        return new Itr();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Sequence) {
            final Sequence<?> s = (Sequence<?>) other;
            return this.spanEquals(s) && this.subs.equals(s.subs);
        }
        return false;
    }

    public static class Parent extends Sequence<Sequence<?>> {

        public Parent(
                final JelType type, final List<Sequence<?>> subs) {
            super(type, subs);
        }

        public Parent(
                final JelType type, final Span<?> s, final Span<?> e, final List<Sequence<?>> subs) {
            super(type, s, e, subs);
        }

        @Override
        public List<Sequence<?>> subsequences() {
            return this.subs;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public Sequence<Sequence<?>> asParent() {
            return this;
        }

        @Override
        public List<Span<?>> flatten() {
            final List<Span<?>> flat = new ArrayList<>();
            for (final Sequence<?> sub : this.subs) {
                flat.addAll(sub.flatten());
            }
            return flat;
        }
    }

    public static class Primitive extends Sequence<Token> {
        public Primitive(
                final JelType type, final List<Token> subs) {
            super(type, subs);
        }

        public Primitive(
                final JelType type, final Span<?> s, final Span<?> e, final List<Token> subs) {
            super(type, s, e, subs);
        }

        @Override
        public List<Token> tokens() {
            return this.subs;
        }

        @Override
        public boolean isPrimitive() {
            return true;
        }

        @Override
        public Sequence<Token> asPrimitive() {
            return this;
        }

        @Override
        public List<Span<?>> flatten() {
            return Collections.singletonList(this);
        }
    }

    public static class Combined extends Sequence<Span<?>> {
        public Combined(final JelType type, final List<Span<?>> subs) {
            super(type, subs);
        }

        public Combined(final JelType type, final Span<?> s, final Span<?> e, final List<Span<?>> subs) {
            super(type, s, e, subs);
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public List<Span<?>> flatten() {
            final List<Span<?>> flat = new ArrayList<>();
            for (final Span<?> span : this.subs) {
                if (span instanceof Token) {
                    flat.add(span);
                } else if (span instanceof Sequence) {
                    flat.addAll(((Sequence<?>) span).flatten());
                }
            }
            return flat;
        }
    }

    public class Itr implements Iterator<T> {
        private int idx = -1;

        public int index() {
            return this.idx;
        }

        @Override
        public boolean hasNext() {
            return this.idx + 1 < subs.size();
        }

        @Override
        public T next() {
            return subs.get(++this.idx);
        }

        public @Nullable T peek() {
            return this.peek(1);
        }

        public @Nullable T peek(final int amount) {
            final int peekIdx = this.idx + amount;
            return !subs.isEmpty() && peekIdx >= 0 && peekIdx < subs.size()
                ? subs.get(peekIdx) : null;
        }
    }
}
