package xjs.jel.expression;

import xjs.core.Json;
import xjs.core.JsonArray;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.ModifyingOperator;
import xjs.jel.sequence.ModifyingOperatorSequence;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.jel.sequence.Sequence;

import java.util.List;

public class OperatorExpression
        extends Sequence.Parent implements Expression {

    public OperatorExpression(
            final List<Sequence<?>> subs) {
        super(JelType.NUMBER_EXPRESSION, subs);
    }

    protected OperatorExpression(
            final JelType type, final List<Sequence<?>> subs) {
        super(type, subs);
    }

    @Override
    public JsonValue apply(final JelContext ctx) throws JelException {
        return this.calculate(ctx, this.iterator());
    }

    protected JsonValue calculate(final JelContext ctx, final Sequence<Sequence<?>>.Itr itr) throws JelException {
        return this.calculate(ctx, itr, this.getNextValue(ctx, itr));
    }

    protected JsonValue calculate(
            final JelContext ctx,
            final Sequence<Sequence<?>>.Itr itr,
            final JsonValue first) throws JelException {
        JsonValue out = first;
        while (itr.hasNext()) {
            out = apply(this.getNextOperator(ctx, itr), out, this.getNextValue(ctx, itr));
        }
        return out;
    }

    protected JsonValue getNextValue(
            final JelContext ctx,
            final Sequence<Sequence<?>>.Itr itr) throws JelException {
        final Sequence<?> next = itr.next();
        if (next == null) {
            throw new JelException("no values")
                .withSpan(ctx, this);
        }
        if (next instanceof ModifyingOperatorSequence) {
            final ModifyingOperatorSequence m = (ModifyingOperatorSequence) next;
            if (m.op != ModifyingOperator.INVERT) {
                throw new JelException("Unsupported modifier in operator expression")
                    .withSpan(ctx, m)
                    .withDetails("Hint: operator expression only supports '-' modifier");
            }
            final Sequence<?> after = itr.next();
            if (after == null) {
                throw new JelException("missing number after sign")
                    .withSpan(ctx, next);
            } else if (!(after instanceof Expression)) {
                throw new JelException("Illegal operand")
                    .withSpan(ctx, after);
            }
            return Json.value(-((Expression) after).applyAsNumber(ctx));
        }
        if (!(next instanceof Expression)) {
            throw new JelException("Illegal operand")
                .withSpan(ctx, next);
        }
        return ((Expression) next).apply(ctx);
    }

    protected Operator getNextOperator(
            final JelContext ctx, final Sequence<Sequence<?>>.Itr itr) throws JelException {
        // hasNext called by calculate
        final Sequence<?> next = itr.next();
        if (next instanceof OperatorSequence) {
            return ((OperatorSequence) next).op;
        }
        throw new JelException("not an operator")
            .withSpan(ctx, next);
    }

    private static JsonValue apply(
            final Operator operator, final JsonValue lhs, final JsonValue rhs) throws JelException {
        switch (operator) {
            case ADD: return add(lhs, rhs);
            case SUBTRACT: return subtract(lhs, rhs);
            case MULTIPLY: return multiply(lhs, rhs);
        }
        throw new JelException(
            new IllegalStateException("unknown operator: " + operator));
    }

    protected static JsonValue add(final JsonValue lhs, final JsonValue rhs) {
        if (lhs.isContainer()) {
            if (lhs.isObject() && rhs.isObject()) {
                return Json.object()
                    .addAll(lhs.asObject())
                    .addAll(rhs.asObject());
            }
            return Json.array()
                .addAll(lhs.asContainer())
                .add(rhs);
        } else if (lhs.isString()) {
            return Json.value(lhs.asString() + rhs.intoString());
        }
        return Json.value(lhs.intoDouble() + rhs.intoDouble());
    }

    protected static JsonValue subtract(final JsonValue lhs, final JsonValue rhs) {
        if (lhs.isObject()) {
            if (rhs.isObject()) {
                final JsonObject o = new JsonObject().addAll(lhs.asObject());
                for (final JsonObject.Member m : rhs.asObject()) {
                    final JsonValue removed = o.get(m.getKey());
                    if (removed == null) {
                        continue;
                    }
                    if (removed.isContainer()) {
                        o.set(m.getKey(), subtract(removed, m.getValue()));
                    } else {
                        o.remove(m.getKey());
                    }
                }
                return o;
            } else if (isKeys(rhs)) {
                final JsonObject o = new JsonObject().addAll(lhs.asObject());
                rhs.intoArray().forEach(v -> o.remove(v.asString()));
                return o;
            }
        }
        if (lhs.isContainer()) {
            return lhs.shallowCopy()
                .asContainer()
                .removeAll(rhs.intoContainer().values());
        } else if (lhs.isString()) {
            return Json.value(lhs.toString()
                .replace(rhs.toString(), ""));
        }
        return Json.value(lhs.intoDouble() - rhs.intoDouble());
    }

    protected static boolean isKeys(final JsonValue value) {
        if (value.isString()) {
            return true;
        } else if (value.isArray()) {
            for (final JsonValue element : value.asArray().visitAll()) {
                if (!element.isString()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    protected static JsonValue multiply(final JsonValue lhs, final JsonValue rhs) {
        final double count = rhs.intoDouble();
        if (lhs.isObject()) {
            final JsonArray a = new JsonArray();
            for (int i = 0; i < count; i++) {
                a.add(lhs.deepCopy());
            }
            return a;
        } else if (lhs.isArray()) {
            final JsonArray a = new JsonArray();
            for (int i = 0; i < count; i++) {
                for (final JsonValue v : lhs.asArray().visitAll()) {
                    a.add(v.deepCopy());
                }
            }
            return a;
        } else if (lhs.isString()) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(lhs.asString());
            }
            return Json.value(sb.toString());
        }
        return Json.value(lhs.intoDouble() * count);
    }
}
