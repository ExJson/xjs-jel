package xjs.jel.modifier;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonLiteral;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.OperatorExpression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConditionalModifier
        extends Sequence.Combined implements Modifier {
    private final @Nullable OperatorExpression inline;

    public ConditionalModifier(
            final Token token, final @Nullable OperatorExpression inline) {
        super(JelType.FLAG, buildList(token, inline));
        this.inline = inline;
    }

    private static List<Span<?>> buildList(
            final Token token, final @Nullable OperatorExpression inline) {
        final List<Span<?>> spans = new ArrayList<>();
        spans.add(token);
        if (inline != null) spans.add(inline);
        return spans;
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) throws JelException {
        if (this.inline != null && !this.inline.apply(ctx).intoBoolean()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(member);
    }

    @Override
    public Expression modify(final Expression expression) {
        if (this.inline == null) {
            return expression;
        }
        return ctx -> {
            if (this.inline.apply(ctx).intoBoolean()) {
                return expression.apply(ctx);
            }
            return JsonLiteral.jsonNull();
        };
    }

    @Override
    public JelType getValueType() {
        if (this.inline == null) {
            return JelType.CONDITIONAL;
        }
        return JelType.NONE;
    }

    @Override
    public List<Span<?>> flatten() {
        final List<Span<?>> flat = new ArrayList<>();
        flat.add(new Sequence.Primitive(
            JelType.FLAG, Collections.singletonList((Token)this.subs.get(0))));
        if (this.subs.size() > 1) {
            flat.addAll(((Sequence<?>)this.subs.get(1)).flatten());
        }
        return flat;
    }
}
