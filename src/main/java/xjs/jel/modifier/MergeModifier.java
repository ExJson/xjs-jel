package xjs.jel.modifier;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonLiteral;
import xjs.core.JsonObject;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MergeModifier
        extends Sequence.Primitive implements Modifier {
    private @Nullable JsonPath path;

    public MergeModifier(final Token token) {
        super(JelType.FLAG, buildList(token));
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) throws JelException {
        if (member.getAlias() != null
                && member.getAlias().aliasType() != AliasType.REFERENCE) {
            throw new JelException(
                "cannot merge values--alias was expanded by another modifier")
                .withSpan(this);
        }
        if (member.getAlias() != null) {
            this.doMerge(ctx, member.getExpression());
            return Collections.emptyList();
        }
        final JsonObject in = this.checkObject(member.getExpression().apply(ctx));
        final List<JelMember> out = new ArrayList<>();
        for (final JsonObject.Member m : in) {
            out.add(JelMember.of(m.getKey(), m.getOnly()));
        }
        return out;
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> {
            this.doMerge(ctx, expression);
            return JsonLiteral.jsonNull();
        };
    }

    private void doMerge(
            final JelContext ctx, final Expression exp) throws JelException {
        final JsonObject source = this.checkObject(this.getSource(ctx));
        final JsonObject in = this.checkObject(exp.apply(ctx));
        for (final JsonObject.Member m : in) {
            final JsonValue v = m.getValue().deepCopy();
            v.addFlag(JelFlags.MERGE);
            source.add(m.getKey(), v);
        }
    }

    private JsonValue getSource(final JelContext ctx) throws JelException {
        if (this.path != null) {
            return this.path.getReference(ctx).getOnly();
        }
        return ctx.getParent();
    }

    private JsonObject checkObject(final JsonValue value) throws JelException {
        if (!value.isObject()) {
            throw new JelException("Cannot merge values--not an object")
                .withSpan(this)
                .withDetails(value.toString());
        }
        return value.asObject();
    }

    @Override
    public AliasType getAliasType() {
        return AliasType.REFERENCE;
    }

    @Override
    public void captureAlias(final Alias alias) {
        this.path = alias.path();
    }
}
