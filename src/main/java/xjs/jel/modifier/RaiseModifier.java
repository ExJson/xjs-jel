package xjs.jel.modifier;

import xjs.core.JsonLiteral;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.Token;

public class RaiseModifier
        extends Sequence.Primitive implements Modifier {

    public RaiseModifier(final Token token) {
        super(JelType.FLAG, buildList(token));
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> {
            this.doRaise(ctx, expression);
            return JsonLiteral.jsonNull();
        };
    }

    private void doRaise(
            final JelContext ctx, final Expression exp) throws JelException {
        final JsonValue v = exp.apply(ctx);
        if (!v.isObject()) {
            throw new JelException(format(v)).withSpan(this);
        }
        final JsonObject o = v.asObject();
        final String msg = o.getOptional("msg", RaiseModifier::format).orElse("");
        final JsonValue detailsValue = o.get("details");

        final String details;
        if (detailsValue != null) {
            if (detailsValue.isObject()) {
                final StringBuilder sb = new StringBuilder();
                for (final JsonObject.Member m : detailsValue.asObject()) {
                    sb.append(m.getKey());
                    sb.append(": ");
                    sb.append(format(m.getValue()));
                    sb.append('\n');
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                }
                details = sb.toString();
            } else {
                details = format(detailsValue);
            }
        } else {
            details = null;
        }
        JelException e = new JelException(msg).withSpan(this);
        if (details != null) {
            e = e.withDetails(details);
        }
        throw e;
    }

    private static String format(final JsonValue value) {
        return value.isPrimitive() ? value.intoString() : value.toString();
    }
}
