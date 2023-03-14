package xjs.jel.expression;

import xjs.core.JsonLiteral;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.lang.JelObject;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;

import java.util.List;

public class ConditionalExpression
        extends Sequence.Combined implements Expression {

    public ConditionalExpression(final ContainerToken source, final List<Span<?>> subs) {
        super(JelType.CONDITIONAL, source, source, subs);
    }

    protected ConditionalExpression(
            final JelType type, final ContainerToken source, final List<Span<?>> subs) {
        super(type, source, source, subs);
    }

    @Override
    public JsonValue apply(final JelContext ctx) throws JelException {
        for (final Span<?> span : this.subs) {
            if (!(span instanceof JelMember)) {
                continue;
            }
            final JelMember branch = (JelMember) span;
            if (branch.getAlias().value().applyAsBoolean(ctx)) {
                final List<JelMember> processed = branch.process(ctx);
                if (processed.isEmpty()) {
                    return JsonLiteral.jsonNull();
                } else if (processed.size() == 1) {
                    if (processed.get(0) instanceof Callable) {
                        throw new JelException("Cannot return template from conditional expression")
                            .withSpan(ctx, this)
                            .withDetails("No way to capture expression as value");
                    }
                    return processed.get(0).getValue(ctx);
                }
                throw new JelException("Unsure how to handle multi-member output in conditional--unreachable?")
                    .withSpan(ctx, this)
                    .withDetails("Feature is still in design");
            }
        }
        if (ctx.isStrictPathing()) {
            throw new JelException("No condition matched. Missing default case?")
                .withSpan(ctx, this)
                .withDetails("Hint: this application disallows lenient pathing");
        }
        return JsonLiteral.jsonNull();
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }
}
