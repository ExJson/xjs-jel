package xjs.jel.expression;

import xjs.core.JsonLiteral;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;

import java.util.List;

public class MatchExpression extends ConditionalExpression {
    private final Expression toMatch;

    public MatchExpression(
            final Expression toMatch, final ContainerToken source, final List<Span<?>> subs) {
        super(JelType.MATCH, source, subs);
        this.toMatch = toMatch;
    }

    @Override
    public JsonValue apply(final JelContext ctx) throws JelException {
        final JsonValue a = this.toMatch.apply(ctx);
        for (final Span<?> span : this.subs) {
            if (!(span instanceof JelMember)) {
                continue;
            }
            final JelMember branch = (JelMember) span;
            final Expression value = branch.getAlias().value();
            if (value instanceof DefaultCaseExpression || a.matches(value.apply(ctx))) {
                final List<JelMember> processed = branch.process(ctx);
                if (processed.isEmpty()) {
                    return JsonLiteral.jsonNull();
                } else if (processed.size() == 1) {
                    return processed.get(0).getValue(ctx);
                }
                throw new JelException("Unsure how to handle multi-member output in match--unreachable?")
                    .withSpan(ctx, this)
                    .withDetails("Feature is still in design");
            }
            if (ctx.isStrictPathing()) {
                throw new JelException("No condition matched. Missing default case?")
                    .withSpan(ctx, this)
                    .withDetails("Hint: this application disallows lenient pathing");
            }
        }
        return JsonLiteral.jsonNull();
    }
}
