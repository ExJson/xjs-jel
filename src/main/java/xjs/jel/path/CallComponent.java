package xjs.jel.path;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonArray;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.lang.CallableFacade;
import xjs.jel.lang.JelFunctions;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.expression.Expression;
import xjs.jel.expression.TupleExpression;
import xjs.jel.scope.CallableAccessor;
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
        Callable callable = this.findCallable(ctx, accessor, parent);
        if (callable == null) {
            return Collections.emptyList();
        }
        final List<JsonValue[]> argsList = this.getArgs(ctx);
        Expression exp = callable;
        for (final JsonValue[] args : argsList) {
            if (!(exp instanceof Callable)) {
                return Collections.emptyList();
            }
            try {
                exp = ((Callable) exp).call(parent, ctx, args);
            } catch (final JelException e) {
                throw e.withSpan(ctx, this);
            }
        }
        final JsonValue r;
        if (exp instanceof Callable) {
            r = new CallableFacade((Callable) exp);
        } else {
            r = exp.apply(ctx);
        }
        return Collections.singletonList(new JsonReference(r));
    }

    private @Nullable Callable findCallable(
            final JelContext ctx,
            final ReferenceAccessor accessor,
            final JsonValue parent) {
        Callable c;
        if (parent instanceof CallableAccessor) {
            c = ((CallableAccessor) parent).getCallable(this.key);
            if (c != null) {
                return c;
            }
        }
        if (accessor != null) {
            c = accessor.getCallable(this.key);
            if (c != null) {
                return c;
            }
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
