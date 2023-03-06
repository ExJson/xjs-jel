package xjs.jel.serialization.sequence;

import org.jetbrains.annotations.Nullable;
import xjs.jel.Alias;
import xjs.jel.JelMember;
import xjs.jel.destructuring.DestructurePattern;
import xjs.jel.exception.JelException;
import xjs.jel.expression.DefaultCaseExpression;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.expression.OperatorExpression;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.modifier.Modifier;
import xjs.jel.sequence.AliasType;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;

import java.util.Collections;
import java.util.List;

public class KeyParser extends ParserModule {
    
    public KeyParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public void parse(
            final JelMember.Builder builder,
            final ContainerToken.Itr itr) throws JelException {
        this.parse(builder, itr, null);
    }

    public void parse(
            final JelMember.Builder builder,
            final ContainerToken.Itr itr,
            final @Nullable AliasType forcedAlias) throws JelException {
        this.whitespaceCollector().append(builder, itr);

        final int keyIdx = this.endOfKey(itr);
        final int aliasIdx = this.endOfAlias(itr, keyIdx);
        final List<Modifier> modifiers =
            this.readModifiers(builder, itr, keyIdx, aliasIdx);

        final AliasType type = getAliasType(modifiers, forcedAlias);
        this.readAlias(builder, itr, type, aliasIdx, keyIdx);

        if (builder.alias() != null) {
            for (final Modifier modifier : modifiers) {
                modifier.captureAlias(builder.alias());
            }
        }
        itr.skipTo(keyIdx + 1);
    }

    protected int endOfKey(final ContainerToken.Itr itr) throws JelException {
        final Token first = itr.peek();
        Token peek = first;
        int lastSignificant = 0;
        int amount = 0;
        while (peek != null) {
            if (peek.isSymbol(':')) {
                return itr.getIndex() + lastSignificant;
            } else if (JelType.isSignificant(peek)) {
                lastSignificant = amount;
            }
            peek = itr.peek(++amount);
        }
        throw new JelException("expected key")
            .withSpan(first != null ? first : itr.getParent())
            .withDetails("each delimited key-value pair must be separated by ':'");
    }

    protected List<Modifier> readModifiers(
            final JelMember.Builder builder, final ContainerToken.Itr itr,
            final int keyIdx, final int aliasIdx) throws JelException {
        if (keyIdx == aliasIdx) {
            return Collections.emptyList();
        }
        final int start = itr.getIndex();
        itr.skipTo(aliasIdx + 2);
        this.modifierParser().parse(builder, itr);
        itr.skipTo(start);
        return builder.modifiers();
    }

    protected int endOfAlias(
            final ContainerToken.Itr itr, final int e) {
        final ContainerToken tokens = (ContainerToken) itr.getParent();
        for (int i = itr.getIndex(); i < e - 1; i++) {
            if (tokens.get(i).isSymbol('>')
                    && tokens.get(i + 1).isSymbol('>')) {
                return i;
            }
        }
        return e;
    }

    protected AliasType getAliasType(
            final List<Modifier> modifiers,
            final @Nullable AliasType forcedType) throws JelException {
        AliasType type = AliasType.NONE;
        Modifier requiredBy = null;
        for (final Modifier modifier : modifiers) {
            final AliasType expected = modifier.getAliasType();
            if (expected == AliasType.NONE) {
                continue;
            }
            if (type != AliasType.NONE && type != expected) {
                throw new JelException("illegal modifier sequence")
                    .withSpan((Span<?>) requiredBy)
                    .withSpan((Span<?>) modifier)
                    .withDetails("incompatible alias types expected: " + type + ", " + expected);
            } else if (forcedType != null) {
                throw new JelException("illegal modifier sequence")
                    .withSpan((Span<?>) modifier)
                    .withDetails("A parent value is expecting an alias of type " + forcedType);
            } else {
                type = expected;
                requiredBy = modifier;
            }
        }
        return forcedType != null ? forcedType : type;
    }

    protected void readAlias(
            final JelMember.Builder builder, final ContainerToken.Itr itr,
            final AliasType type, final int aliasIdx, final int keyIdx) throws JelException {
        switch (type) {
            case DESTRUCTURE:
                this.readDestructure(builder, itr, aliasIdx);
                break;
            case REFERENCE:
                this.readReference(builder, itr, aliasIdx);
                break;
            case VALUE:
                this.readValue(builder, itr, aliasIdx);
                break;
            case OPERATOR:
                this.readOperator(builder, itr, aliasIdx);
                break;
            default:
                this.readLiteral(builder, itr, aliasIdx, keyIdx);
        }
    }

    protected void readDestructure(
            final JelMember.Builder builder, final ContainerToken.Itr itr,
            final int e) throws JelException {
        final Token peek = itr.peek();
        if (peek instanceof ContainerToken) {
            final ContainerToken token = (ContainerToken) itr.next();
            final DestructurePattern exp =
                this.destructureParser().parse(token);
            builder.alias(Alias.of(exp));
            if (itr.getIndex() == e) {
                return;
            }
            throw new JelException("expected end of alias")
                .withSpan(exp)
                .withDetails("a modifier requires this alias to be a destructure pattern");
        }
        JelException ex = new JelException("expected destructure expression")
            .withDetails("a modifier requires this alias to be a destructure pattern");
        if (peek == null) {
            final Token current = itr.peek(0);
            if (current != null) {
                ex = ex.withSpan(current);
            }
        } else {
            ex = ex.withSpan(peek);
        }
        throw ex;
    }

    protected void readReference(
            final JelMember.Builder builder, final ContainerToken.Itr itr,
            final int e) throws JelException {
        final ReferenceExpression exp = this.referenceParser().parse(itr);
        if (itr.getIndex() != e) {
            throw new JelException("expected end of alias")
                .withSpan(exp)
                .withDetails("a modifier requires this alias to be a single reference");
        }
        builder.alias(Alias.of(exp));
    }

    protected void readValue(
            final JelMember.Builder builder, final ContainerToken.Itr itr,
            final int e) throws JelException {
        final DefaultCaseExpression defaultCase =
            this.operatorParser().tryParseDefaultCase(itr, e);
        final Expression exp;
        if (defaultCase != null) {
            exp = defaultCase;
        } else {
            exp = this.elementParser().parseInline(itr, e);
        }
        if (itr.getIndex() != e) {
            throw new JelException("expected end of alias")
                .withSpan((Span<?>) exp);
        }
        builder.alias(Alias.of(exp));
    }

    protected void readOperator(
            final JelMember.Builder builder, final ContainerToken.Itr itr,
            final int e) throws JelException {
        final DefaultCaseExpression defaultCase =
            this.operatorParser().tryParseDefaultCase(itr, e);
        final OperatorExpression exp;
        if (defaultCase != null) {
            exp = defaultCase;
        } else {
            exp = this.operatorParser().parse(itr, e);
        }
        if (itr.getIndex() != e) {
            throw new JelException("expected end of alias")
                .withSpan(exp);
        }
        builder.alias(Alias.of(exp));
    }

    protected void readLiteral(
            final JelMember.Builder builder, final ContainerToken.Itr itr,
            final int aliasIdx, final int keyIdx) {
        if (itr.getIndex() < aliasIdx || aliasIdx == keyIdx) {
            final LiteralExpression exp =
                this.expressionParser().literalPrimitive(itr, aliasIdx);
            builder.alias(Alias.of(exp));
        }
    }
}
