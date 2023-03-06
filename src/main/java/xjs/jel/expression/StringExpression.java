package xjs.jel.expression;

import xjs.core.Json;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ParsedToken;

import java.util.List;

public class StringExpression
        extends Sequence.Combined implements Expression {

    public StringExpression(final List<Span<?>> subs) {
        super(JelType.STRING_EXPRESSION, subs);
    }

    @Override
    public JsonValue apply(final JelContext ctx) throws JelException {
        final StringBuilder sb = new StringBuilder();
        for (final Span<?> sub : this.subs) {
            if (sub instanceof ParsedToken) {
                sb.append(((ParsedToken) sub).parsed());
            } else if (sub instanceof Expression) {
                final JsonValue v = ((Expression) sub).apply(ctx);
                sb.append(v.isPrimitive() ? v.intoString() : v);
            }
        }
        return Json.value(sb.toString());
    }
}
