package xjs.jel.modifier;

import xjs.core.JsonValue;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;

public class DefaultsModifier
        extends Sequence.Parent implements Modifier {
    private final ReferenceExpression path;

    public DefaultsModifier(final ReferenceExpression path) {
        super(path.type(), buildList(path));
        this.path = path;
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> {
            final JsonValue defaults = this.path.get(ctx);
            if (!defaults.isObject()) {
                throw new JelException("Cannot copy defaults from non-object value")
                    .withSpan(ctx, this.path)
                    .withDetails("Cannot copy from: " + defaults);
            }
            final JsonValue out = expression.apply(ctx);
            if (!out.isObject()) {
                JelException e = new JelException("Cannot copy defaults into non-object value")
                    .withSpan(ctx, this.path)
                    .withDetails("Cannot copy: " + defaults);
                if (expression instanceof Span<?>) {
                    e = e.withSpan(ctx, (Span<?>) expression);
                }
                throw e;
            }
            return out.asObject().setDefaults(defaults.asObject());
        };
    }
}
