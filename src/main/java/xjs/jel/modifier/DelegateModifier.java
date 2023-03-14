package xjs.jel.modifier;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonValue;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.expression.Expression;
import xjs.jel.path.KeyComponent;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;

import java.util.Collections;
import java.util.List;

public class DelegateModifier
        extends Sequence.Parent implements Modifier {
    private final ReferenceExpression ref;

    public DelegateModifier(final ReferenceExpression ref) {
        super(JelType.DELEGATE, buildList(ref));
        this.ref = ref;
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> {
            final JsonValue value = expression.apply(ctx);
            Callable c = this.ref.getCallable(ctx, ctx.getScope(), ctx.peekParent());
            if (c == null) {
                final String key = this.getKey();
                throw new JelException("Callable not in scope: " + key)
                    .withSpan(ctx, this.subs.get(0))
                    .withDetails("Expected a template or function named '" + key + "'");
            }
            final Expression out;
            try {
                out = c.call(ctx.getParent(), ctx, value);
            } catch (final JelException e) {
                throw e.withSpan(ctx, this);
            }
            if (out instanceof Callable) {
                JelException e = new JelException("Unexpected callable returned by delegate")
                    .withSpan(ctx, this);
                if (out instanceof Span<?>) {
                    e = e.withSpan((Span<?>) out);
                }
                throw e;
            }
            return out.apply(ctx);
        };
    }

    protected @Nullable String getKey() {
        final List<? extends Span<?>> spans = this.ref.spans();
        if (spans.isEmpty()) {
            return null;
        }
        final Span<?> last = spans.get(spans.size() - 1);
        if (!(last instanceof KeyComponent)) {
            return null;
        }
        return ((KeyComponent) last).key;
    }

    @Override
    public List<Span<?>> flatten() {
        return Collections.singletonList(this);
    }
}
