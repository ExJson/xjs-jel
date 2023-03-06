package xjs.jel.modifier;

import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.destructuring.DestructurePattern;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.lang.JelObject;
import xjs.jel.sequence.AliasType;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.List;

public class DestructureModifier
        extends Sequence.Primitive implements Modifier {

    public DestructureModifier(final Token token) {
        super(JelType.FLAG, buildList(token));
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) throws JelException {
        if (member.getAlias().aliasType() != AliasType.DESTRUCTURE) {
            throw new JelException(
                "cannot destructure--alias was expanded by another modifier")
                .withSpan(this);
        }
        final JsonValue v = checkValue(ctx, member.getExpression());
        final DestructurePattern pattern = member.getAlias().pattern();
        final JelObject o = new JelObject();
        pattern.destructure(v.asContainer(), o);
        return membersOf(o);
    }

    private static JsonValue checkValue(
            final JelContext ctx, final Expression exp) throws JelException {
        final JsonValue v = exp.apply(ctx);
        if (v.isPrimitive()) {
            JelException e = new JelException("cannot destructure primitives")
                .withDetails(v.toString());
            if (exp instanceof Span<?>) {
                e = e.withSpan((Span<?>) exp);
            }
            throw e;
        }
        return v;
    }

    private static List<JelMember> membersOf(final JsonObject o) {
        final List<JelMember> members = new ArrayList<>();
        for (final JsonObject.Member m : o) {
            // todo: leaking access -- member does not track access
            members.add(JelMember.of(m.getKey(), m.getOnly()));
        }
        if (o instanceof JelObject) {
            members.addAll(((JelObject) o).getCallables());
        }
        return members;
    }

    @Override
    public Expression modify(final Expression expression) {
        return ctx -> {
            throw new JelException("Cannot capture destructuring modifier")
                .withSpan(this);
        };
    }

    @Override
    public AliasType getAliasType() {
        return AliasType.DESTRUCTURE;
    }

    @Override
    public boolean canBeCaptured() {
        return false;
    }
}
