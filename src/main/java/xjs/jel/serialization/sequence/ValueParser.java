package xjs.jel.serialization.sequence;

import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.expression.StringExpression;
import xjs.jel.sequence.JelType;
import xjs.jel.serialization.token.Retokenizer;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;
import xjs.serialization.util.StringContext;

import java.util.List;

public class ValueParser extends ParserModule {

    public ValueParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public Expression parse(final ContainerToken.Itr itr, final int e) throws JelException {
        return this.parse(itr, e, false);
    }

    public Expression parse(
            ContainerToken.Itr itr, int e, final boolean inlined) throws JelException {
        if (itr.getIndex() == e - 1) {
            final Token peek = itr.peek();
            if (peek instanceof StringToken) {
                itr.next();
                final ContainerToken inspected = Retokenizer.inspect(
                    itr.getReference(), (StringToken) peek, StringContext.VALUE);
                itr = inspected.iterator();
                e = inspected.size();
            }
        }
        final Expression exp = this.parseInternal(itr, e, inlined);
        this.streamAnalyzer().checkDangling(itr, e);
        return this.tryInline(exp);
    }

    protected Expression parseInternal(
            final ContainerToken.Itr itr, final int e, final boolean inlined) throws JelException {
        switch (this.typeOfValue(itr, e, inlined)) {
            case NUMBER_EXPRESSION:
                return this.operatorParser().parse(itr, e);
            case STRING_EXPRESSION:
                return this.expressionParser().stringExpression(itr, e);
            case OBJECT:
                return this.objectParser().parse((ContainerToken) itr.next());
            case ARRAY:
                return this.arrayParser().parse((ContainerToken) itr.next());
        }
        return this.expressionParser().literalPrimitive(itr, e);
    }

    protected JelType typeOfValue(
            final ContainerToken.Itr itr, final int e, final boolean inlined) {
        final Token peek = itr.peek();
        if (peek == null) {
            return JelType.NONE;
        }
        if (!inlined) { // disallow starting with {}[] in regular values
            if (peek.type() == TokenType.BRACES) {
                return JelType.OBJECT;
            } else if (peek.type() == TokenType.BRACKETS) {
                return JelType.ARRAY;
            }
        }
        if (this.operatorParser().isOperatorExpression(itr, e, inlined)) {
            return JelType.NUMBER_EXPRESSION; // type doesn't matter
        } else if (this.expressionParser().isStringExpression(itr, e)) {
            return JelType.STRING_EXPRESSION;
        } else if (this.referenceParser().isReference(itr, peek, 1)) {
            return JelType.REFERENCE;
        }
        if (inlined && itr.getIndex() == e - 1) {
            if (peek.type() == TokenType.BRACES) {
                return JelType.OBJECT;
            } else if (peek.type() == TokenType.BRACKETS) {
                return JelType.ARRAY;
            }
        }
        return JelType.NONE;
    }

    protected Expression tryInline(final Expression exp) {
        if (exp instanceof StringExpression) {
            final List<? extends Span<?>> subs = ((StringExpression) exp).spans();
            if (subs.size() == 1) {
                final Span<?> first = subs.get(0);
                if (first instanceof ReferenceExpression) {
                    return (Expression) first;
                }
            }
        }
        return exp;
    }
}
