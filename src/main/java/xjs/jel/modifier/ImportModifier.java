package xjs.jel.modifier;

import xjs.core.JsonArray;
import xjs.core.JsonCopy;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelFlags;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.lang.JelReflection;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportModifier
        extends Sequence.Primitive implements Modifier {
    public ImportModifier(final Token token) {
        super(JelType.FLAG, buildList(token));
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) throws JelException {
        if (member.isModified()) {
            // reloading files not yet supported (new operator?)
            return Collections.singletonList(member);
        }
        final JsonValue rhs = member.getExpression().apply(ctx);
        final JsonValue imports = this.getImportValues(ctx, rhs, member.hasFlag(JelFlags.VAR));
        if (member.getAlias() != null) {
            member.setExpression(LiteralExpression.of(imports));
            return Collections.singletonList(member);
        }
        final Iterable<JsonValue> importsArray = imports.isArray()
            ? imports.asArray() : Collections.singleton(imports);

        final List<JelMember> members = new ArrayList<>();
        for (final JsonValue imported : importsArray) {
            if (!imported.isObject()) {
                throw new JelException("Non-object import must have alias")
                    .withSpan(ctx, this);
            }
            members.addAll(JelReflection.jelMembers(imported.asObject()));
        }
        return members;
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> this.getImportValues(ctx, expression.apply(ctx), false);
    }

    private JsonValue getImportValues(
            final JelContext ctx, final JsonValue rhs, final boolean isVar) throws JelException {
        if (rhs.isPrimitive()) {
            return this.tryGetImport(ctx, rhs.intoString(), isVar);
        }
        final JsonArray imports = new JsonArray();
        for (final JsonValue file : rhs.intoArray()) {
            imports.add(this.tryGetImport(ctx, file.intoString(), isVar));
        }
        return imports;
    }

    private JsonValue tryGetImport(
            final JelContext ctx, final String path, final boolean isVar) throws JelException {
        if (ctx.isLoading(path)) {
            throw new JelException("Illegal cyclical reference")
                .withSpan(ctx, this)
                .withDetails("Hint: this file is also being loaded by " + path);
        }
        try {
            if (isVar) {
                return ctx.getImport(path);
            }
            return ctx.getImport(path)
                .copy(JsonCopy.RECURSIVE | JsonCopy.FORMATTING);
        } catch (final JelException e) {
            throw e.withSpan(this);
        }
    }
}
