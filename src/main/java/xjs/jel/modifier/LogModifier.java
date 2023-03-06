package xjs.jel.modifier;

import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ArrayExpression;
import xjs.jel.expression.Expression;
import xjs.jel.expression.ObjectExpression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.Token;

public class LogModifier
        extends Sequence.Primitive implements Modifier {
    private final boolean error;

    public LogModifier(final boolean error, final Token... tokens) {
        super(JelType.FLAG, buildList(tokens));
        this.error = error;
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> this.log(ctx, expression);
    }

    private JsonValue log(
            final JelContext ctx, final Expression exp) throws JelException {
        final JsonValue v = exp.apply(ctx);
        if (exp instanceof ObjectExpression) {
            for (final JsonObject.Member m : v.asObject()) {
                this.doLog(ctx, m.getKey() + ": " + format(m.getOnly()));
            }
        } else if (exp instanceof ArrayExpression) {
            for (final JsonValue e : v.asArray().visitAll()) {
                this.doLog(ctx, format(e));
            }
        } else {
            this.doLog(ctx, format(v));
        }
        return v;
    }

    private static String format(final JsonValue value) {
        return value.isPrimitive() ? value.intoString() : value.toString();
    }

    private void doLog(final JelContext ctx, final String msg) {
        if (this.error) {
            ctx.error(msg);
        } else {
            ctx.log(msg);
        }
    }
}
