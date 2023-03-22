package xjs.jel.modifier;

import xjs.core.JsonValue;
import xjs.jel.Alias;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.sequence.AliasType;
import xjs.jel.sequence.JelType;

import java.util.Collections;
import java.util.List;

public interface Modifier {

    default List<JelMember> modify(
            final JelContext ctx, final JelMember member) throws JelException {
        if (!member.isModified()) {
            member.setExpression(this.modify(member.getExpression()));
        }
        return Collections.singletonList(member);
    }

    default Expression modify(final JsonValue value) {
        return this.modify(LiteralExpression.of(value));
    }

    default Expression modify(final Expression expression) {
        return expression;
    }

    default AliasType getAliasType() {
        return AliasType.NONE;
    }

    default void captureAlias(final Alias alias) {}

    default boolean requiresAlias() {
        return false;
    }

    default boolean capturesModifiers() {
        return false;
    }

    default void captureModifier(final Modifier modifier) {}

    default List<Modifier> getCaptures() {
        return Collections.emptyList();
    }

    default boolean canBeCaptured(final Modifier by) {
        return !this.changesValueType();
    }

    default JelType getValueType() {
        return JelType.NONE;
    }

    default boolean changesValueType() {
        return this.getValueType() != JelType.NONE;
    }

    // capture logic handled by parser
    static Expression modify(
            final Expression template, final List<Modifier> captures) {
        Expression exp = template;
        for (final Modifier modifier : captures) {
            exp = modifier.modify(exp);
        }
        return exp;
    }
}
