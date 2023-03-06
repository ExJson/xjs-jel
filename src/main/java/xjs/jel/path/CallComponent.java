package xjs.jel.path;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonArray;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.lang.JelFunctions;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.expression.Expression;
import xjs.jel.expression.TupleExpression;
import xjs.jel.scope.ReferenceAccessor;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CallComponent extends PathComponent {
    public final String key;
    public final @Nullable List<JsonArray> parsed;
    public final List<TupleExpression> raw;

    public CallComponent(
            final ParsedToken key, final List<TupleExpression> argumentChain) {
        this(key, null, argumentChain);
    }

    public CallComponent(
            final ParsedToken key,
            final @Nullable List<JsonArray> parsed,
            final List<TupleExpression> argumentChain) {
        super(JelType.CALL, buildList(key, argumentChain));
        this.key = key.parsed();
        this.raw = argumentChain;
        this.parsed = parsed;
    }

    private static List<Span<?>> buildList(
            final ParsedToken key, final List<TupleExpression> argumentChain) {
        final List<Span<?>> list = new ArrayList<>();
        list.add(key);
        list.addAll(argumentChain);
        return list;
    }

    @Override
    public List<JsonReference> getAll(
            final JelContext ctx,
            final @Nullable ReferenceAccessor accessor,
            final JsonValue parent) throws JelException {
        try {
            return this.tryGetAll(ctx, accessor, parent);
        } catch (final JelException e) {
            throw e.withSpan(this);
        }
    }

    private List<JsonReference> tryGetAll(
            final JelContext ctx,
            final @Nullable ReferenceAccessor accessor,
            final JsonValue parent) throws JelException {
        Callable callable = this.getCallable(ctx, accessor);
        if (callable == null) {
            return Collections.emptyList();
        }
        final List<JsonValue[]> argsList = this.getArgs(ctx);
        Expression e = callable;
        for (final JsonValue[] args : argsList) {
            if (!(e instanceof Callable)) {
                return Collections.emptyList();
            }
            e = ((Callable) e).call(parent, ctx, args);
        }
        if (e instanceof Callable) {
            return Collections.emptyList();
        }
        final JsonValue r = e.apply(ctx);
        return Collections.singletonList(new JsonReference(r));
    }

    private @Nullable Callable getCallable(
            final JelContext ctx,
            final @Nullable ReferenceAccessor accessor) {
        Callable c = accessor != null
            ? accessor.getCallable(this.key) : null;
        if (c != null) {
            return c;
        }
        if (accessor != ctx.getScope()) {
            c = ctx.getScope().getCallable(this.key);
            if (c != null) {
                return c;
            }
        }
        return JelFunctions.lookup(ctx.getPrivilege(), this.key);
    }

    private List<JsonValue[]> getArgs(
            final JelContext ctx) throws JelException {
        if (this.parsed != null) {
            return this.parsed.stream()
                .map(CallComponent::toArray)
                .collect(Collectors.toList());
        }
        final List<JsonValue[]> args = new ArrayList<>();
        for (final TupleExpression ids : this.raw) {
            args.add(toArray(ids.apply(ctx)));
        }
        return args;
    }

    private static JsonValue[] toArray(final JsonArray a) {
        return a.stream().toArray(JsonValue[]::new);
    }

    @Override
    protected boolean acceptsNullAccessor() {
        return true;
    }

    @Override
    public List<Span<?>> flatten() {
        final List<Span<?>> flat = new ArrayList<>();
        flat.add(new Sequence.Primitive(
            JelType.CALL, Collections.singletonList((Token) this.subs.get(0))));
        for (final TupleExpression tuple : this.raw) {
            flat.addAll(tuple.flatten());
        }
        return flat;
    }
}