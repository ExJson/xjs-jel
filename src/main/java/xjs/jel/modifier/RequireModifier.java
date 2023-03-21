package xjs.jel.modifier;

import xjs.core.JsonValue;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.Token;

public class RequireModifier
        extends Sequence.Primitive implements Modifier {

    public RequireModifier(final Token token) {
        super(JelType.FLAG, buildList(token));
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> {
            final JsonValue value = expression.apply(ctx);
            for (final JsonValue required : value.intoArray()) {
                if (!required.isString()) {
                    throw new JelException("Not a string: " + required)
                        .withSpan(ctx, this)
                        .withDetails("Hint: expected a file or directory to load after this file");
                }
                try {
                    ctx.require(required.asString());
                } catch (final JelException e) {
                    throw e.withSpan(ctx, this);
                }
            }
            return value;
        };
    }
}
