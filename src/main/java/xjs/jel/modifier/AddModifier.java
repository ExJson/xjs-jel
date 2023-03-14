package xjs.jel.modifier;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonArray;
import xjs.core.JsonLiteral;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.Alias;
import xjs.jel.JelContext;
import xjs.jel.JelFlags;
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

public class AddModifier
        extends Sequence.Primitive implements Modifier {
    private @Nullable JsonPath path;

    public AddModifier(final Token token) {
        super(JelType.FLAG, buildList(token));
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) throws JelException {
        if (member.getAlias().aliasType() != AliasType.REFERENCE) {
            throw new JelException(
                "cannot insert value--alias was expanded by another modifier")
                .withSpan(ctx, this);
        }
        this.doAdd(ctx, member.getExpression());
        return Collections.emptyList();
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> {
            this.doAdd(ctx, expression);
            return JsonLiteral.jsonNull();
        };
    }

    private void doAdd(
            final JelContext ctx, final Expression exp) throws JelException {
        if (this.path == null) {
            throw new IllegalStateException("Alias not captured");
        }
        final JsonReference ref = this.path.getReference(ctx);
        if (!ref.getOnly().isArray()) {
            ref.setOnly(ref.getOnly().intoArray());
        }
        final JsonArray array = ref.getOnly().asArray();
        final JsonValue in = exp.apply(ctx);
        for (final JsonValue v : in.intoArray()) {
            v.addFlag(JelFlags.ADD);
            array.add(v);
        }
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
