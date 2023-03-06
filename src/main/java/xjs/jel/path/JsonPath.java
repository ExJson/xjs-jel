package xjs.jel.path;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonArray;
import xjs.core.JsonContainer;
import xjs.core.JsonLiteral;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.scope.ContainerAccessor;
import xjs.jel.scope.ReferenceAccessor;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonPath extends Sequence<PathComponent> {

    protected JsonPath(final JelType type, final List<PathComponent> subs) {
        super(type, subs);
    }

    protected JsonPath(
            final JelType type, final Span<?> s, final Span<?> e, final List<PathComponent> subs) {
        super(type, s, e, subs);
    }

    public static JsonPath of(final List<PathComponent> components) {
        return new JsonPath(JelType.REFERENCE, components);
    }

    public static JsonPath ofExpansion(final List<PathComponent> components) {
        return new JsonPath(JelType.REFERENCE_EXPANSION, components);
    }

    public JsonValue get(final JelContext ctx) throws JelException {
        return this.get(ctx, ctx.getScope(), ctx.peekParent());
    }

    public JsonReference getReference(final JelContext ctx) throws JelException {
        return this.getReference(ctx, ctx.getScope(), ctx.peekParent());
    }

    public List<JsonReference> getAll(final JelContext ctx) throws JelException {
        return this.getAll(ctx, ctx.getScope(), ctx.peekParent());
    }

    public JsonValue get(final JsonContainer container) {
        try {
            return this.get(JelContext.GLOBAL_CONTEXT, new ContainerAccessor(container), container);
        } catch (final JelException e) {
            throw new IllegalStateException("expression requires context configuration", e);
        }
    }

    public JsonReference getReference(final JsonContainer container) {
        try {
            return this.getReference(JelContext.GLOBAL_CONTEXT, new ContainerAccessor(container), container);
        } catch (final JelException e) {
            throw new IllegalStateException("expression requires context configuration", e);
        }
    }

    public List<JsonReference> getAll(final JsonContainer container) {
        try {
            return this.getAll(JelContext.GLOBAL_CONTEXT, new ContainerAccessor(container), container);
        } catch (final JelException e) {
            throw new IllegalStateException("expression requires context configuration", e);
        }
    }

    public JsonValue get(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) throws JelException {
        final List<JsonReference> refs = this.getAll(ctx, accessor, parent);
        if (refs.size() == 0) {
            if (ctx.isStrictPathing()) {
                throw new JelException("Path does not resolve to any variable")
                    .withSpan(this)
                    .withDetails("Application is configured to disallow lenient pathing");
            }
            return JsonLiteral.jsonNull();
        } else if (refs.size() == 1) {
            return refs.get(0).get();
        }
        return new JsonArray(refs);
    }

    public JsonReference getReference(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) throws JelException {
        final List<JsonReference> refs = this.getAll(ctx, accessor, parent);
        if (refs.isEmpty()) {
            return new JsonReference(JsonLiteral.jsonNull());
        }
        return refs.get(0);
    }

    public @Nullable Callable getCallable(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) throws JelException {
        if (this.subs.isEmpty()) {
            return null;
        }
        final PathComponent last = this.subs.get(this.subs.size() - 1);
        if (!(last instanceof KeyComponent)) {
            return null;
        }
        JsonReference ref = null;
        for (int i = 0; i < this.subs.size() - 1; i++) {
            final PathComponent component = this.subs.get(i);
            final List<JsonReference> refs;
            if (ref == null) {
                refs = component.getAll(ctx, accessor, parent);
            } else {
                final ReferenceAccessor next =
                    ref.getOnly().isContainer()
                        ? new ContainerAccessor(ref.get().asContainer())
                        : null;
                if (next != null || component.acceptsNullAccessor()) {
                    refs = component.getAll(ctx, next, ref.get());
                } else {
                    refs = null;
                }
            }
            if (refs == null || refs.isEmpty()) {
                return null;
            }
            ref = refs.get(0);
        }
        if (ref == null) {
            return ((KeyComponent) last).getCallable(ctx, accessor, parent);
        }
        final ReferenceAccessor next =
            ref.getOnly().isContainer()
                ? new ContainerAccessor(ref.get().asContainer())
                : null;
        return ((KeyComponent) last).getCallable(ctx, next, ref.getOnly());
    }

    public List<JsonReference> getAll(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) throws JelException {
        if (this.subs.isEmpty()) {
            return Collections.emptyList();
        }
        List<JsonReference> all = null;
        for (final PathComponent component : this.subs) {
            if (all == null) {
                all = component.getAll(ctx, accessor, parent);
                continue;
            } else if (all.isEmpty()) {
                return all;
            }
            final List<JsonReference> refs = all;
            all = new ArrayList<>();
            for (final JsonReference ref : refs) {
                final ReferenceAccessor next =
                    ref.getOnly().isContainer()
                        ? new ContainerAccessor(ref.get().asContainer())
                        : null;
                if (next != null || component.acceptsNullAccessor()) {
                    all.addAll(component.getAll(ctx, next, ref.get()));
                }
            }
        }
        return all;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public Sequence<Sequence<?>> asParent() {
        return new Sequence.Parent(this.type, new ArrayList<>(this.subs));
    }

    @Override
    public List<Span<?>> flatten() {
        final List<Span<?>> flat = new ArrayList<>();
        for (final PathComponent sub : this.subs) {
            flat.addAll(sub.flatten());
        }
        return flat;
    }

}
