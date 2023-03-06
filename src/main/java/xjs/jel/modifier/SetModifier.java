package xjs.jel.modifier;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonLiteral;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.Alias;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.path.JsonPath;
import xjs.jel.sequence.AliasType;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.Token;

import java.util.Collections;
import java.util.List;

public class SetModifier
        extends Sequence.Primitive implements Modifier {
    private @Nullable JsonPath path;

    public SetModifier(final Token token) {
        super(JelType.FLAG, buildList(token));
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) throws JelException {
        this.doSet(ctx, member.getExpression());
        return Collections.emptyList();
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> {
            this.doSet(ctx, expression);
            return JsonLiteral.jsonNull();
        };
    }

    private void doSet(
            final JelContext ctx, final Expression exp) throws JelException {
        if (this.path == null) {
            throw new IllegalStateException("Alias not captured");
        }
        final List<JsonReference> refs = this.path.getAll(ctx);
        if (refs.isEmpty()) {
            throw new JelException(
                "cannot set value--undefined variable (not yet supported)")
                .withSpans(this, this.path);
        }
        final JsonValue v = exp.apply(ctx);
        refs.forEach(ref -> ref.set(v));
    }

    @Override
    public AliasType getAliasType() {
        return AliasType.REFERENCE;
    }

    @Override
    public boolean requiresAlias() {
        return true;
    }

    @Override
    public void captureAlias(final Alias alias) {
        this.path = alias.path();
    }
}
