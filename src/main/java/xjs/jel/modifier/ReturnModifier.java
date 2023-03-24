package xjs.jel.modifier;

import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.exception.ReturnException;
import xjs.jel.expression.Expression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.Token;

import java.util.Collections;
import java.util.List;

public class ReturnModifier
        extends Sequence.Primitive implements Modifier {

    public ReturnModifier(final Token token) {
        super(JelType.FLAG, buildList(token));
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) throws JelException {
        if (!member.isModified()) {
            final Expression exp = member.getExpression();
            member.setExpression(givenCtx -> {
                throw new ReturnException(this, exp.apply(givenCtx));
            });
        }
        return Collections.singletonList(member);
    }

    @Override
    public Expression modify(Expression expression) {
        return ctx -> {
            throw new JelException("Illegal return modifier")
                .withSpan(ctx, this)
                .withDetails(
                    "Hint: Cannot return from an aliased value."
                    + "Hint: cannot capture or inline return modifier.");
        };
    }
}
