package xjs.jel.serialization.sequence;

import org.jetbrains.annotations.Nullable;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.modifier.Modifier;
import xjs.jel.modifier.NoInlineModifier;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

public class ElementParser extends ParserModule {

    public ElementParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public JelMember parse(final ContainerToken.Itr itr) throws JelException {
        return this.parse(itr, this.endOfElement(itr));
    }

    public JelMember parse(final ContainerToken.Itr itr, final int e) throws JelException {
        final JelMember.Builder builder = JelMember.builder(JelType.ELEMENT);
        this.parse(builder, itr, e);
        return builder.build();
    }

    public void parse(
            final JelMember.Builder builder, final ContainerToken.Itr itr) throws JelException {
        this.parse(builder, itr, this.endOfElement(itr));
    }

    public void parse(
            final JelMember.Builder builder, final ContainerToken.Itr itr, final int e) throws JelException {
        this.parse(builder, itr, e, false);
    }

    public void parse(
            final JelMember.Builder builder, final ContainerToken.Itr itr,
            final int e, final boolean unpack) throws JelException {
        this.whitespaceCollector().append(builder, itr, e);
        final int valueIdx = this.endOfValue(itr, e);
        final Token peek = itr.peek();
        if (unpack && itr.getIndex() == e - 1
                && peek != null && peek.type() == TokenType.PARENTHESES) {
            final ContainerToken tokens = (ContainerToken) peek;
            itr.next();
            builder.expression(this.readExpression(builder, tokens.iterator(), tokens.size()));
        } else {
            builder.expression(this.readExpression(builder, itr, valueIdx));
        }
        this.streamAnalyzer().checkDangling(itr, valueIdx);
        this.whitespaceCollector().append(builder, itr, e);
    }

    public Expression parseInline(final ContainerToken.Itr itr) throws JelException {
        return this.parseInline(itr, this.endOfElement(itr), false);
    }

    public Expression parseInline(final ContainerToken.Itr itr, final int e) throws JelException {
        return this.parseInline(itr, e, false);
    }
    
    public Expression parseInline(
            final ContainerToken.Itr itr, final int e, final boolean unpack) throws JelException {
        final Token first = itr.peek();
        if (first == null) {
            return this.expressionParser().literalPrimitive(itr, 0);
        } else if (!JelType.isSignificant(first)) {
            final JelMember.Builder builder = JelMember.unformattedBuilder(JelType.ELEMENT);
            this.whitespaceCollector().append(builder, itr, e);
            this.parse(builder, itr, e, true);
            this.whitespaceCollector().append(builder, itr, e);
            return builder.build();
        }
        if (itr.getIndex() == e - 1) {
            if (unpack && first.type() == TokenType.PARENTHESES) {
                final ContainerToken tokens = (ContainerToken) first;
                itr.next();
                return this.parseInline(tokens.iterator(), tokens.size());
            }
            return this.valueParser().parse(itr, e, true);
        }
        final ContainerToken parent = (ContainerToken) itr.getParent();
        if (e == 0 || JelType.isSignificant(parent.get(e - 1))) {
            return this.valueParser().parse(itr, e, true);
        }
        final JelMember.Builder builder = JelMember.unformattedBuilder(JelType.ELEMENT);
        this.parse(builder, itr, e, true);
        this.whitespaceCollector().append(builder, itr, e);
        return builder.build();
    }

    protected int endOfElement(final ContainerToken.Itr itr) {
        final int idx = itr.getIndex();
        Token peek = itr.peek();
        int peekAmount = 1;
        boolean anySignificant = false;

        while (peek != null) {
            if (peek.isSymbol(',') || (anySignificant && peek.type() == TokenType.BREAK)) {
                return idx + peekAmount - 1;
            }
            if (!anySignificant) {
                anySignificant = JelType.isSignificant(peek);
            }
            peek = itr.peek(++peekAmount);
        } // assume there are significant tokens (caller must know up front)
        return ((ContainerToken) itr.getParent()).size();
    }

    protected int endOfValue(
            final ContainerToken.Itr itr, final int e) {
        final ContainerToken tokens = (ContainerToken) itr.getParent();
        if (tokens.size() == 0) {
            return 0;
        }
        for (int i = e - 1; i >= 0; i--) {
            final Token t = tokens.get(i);
            if (JelType.isSignificant(t)) {
                return i + 1;
            }
        }
        return e;
    }

    protected Expression readExpression(
            final JelMember.Builder builder, final ContainerToken.Itr itr,
            final int e) throws JelException {
        final Modifier modifier = this.getValueModifier(builder);
        if (modifier == null) {
            return this.valueParser().parse(itr, e);
        }
        switch (modifier.getValueType()) {
            case MATCH: return this.matchParser().parse(itr, modifier);
            case CONDITIONAL: return this.conditionalParser().parse(itr);
            case STRING:
                this.checkUnsupportedLiteral(builder, itr);
                return this.expressionParser().literalPrimitive(itr, e);
            default: return this.valueParser().parse(itr, e);
        }
    }

    protected @Nullable Modifier getValueModifier(
            final JelMember.Builder builder) throws JelException {
        JelType type = JelType.NONE;
        Modifier requiredBy = null;
        for (final Modifier modifier : builder.modifiers()) {
            final JelType expected = modifier.getValueType();
            if (expected == JelType.NONE) {
                continue;
            }
            if (type != JelType.NONE) {
                if (type == expected) {
                    throw new JelException("illegal modifier sequence")
                        .withSpan((Span<?>) requiredBy)
                        .withSpan((Span<?>) modifier)
                        .withDetails("modifier cannot be duplicated: ");
                }
                throw new JelException("illegal modifier sequence")
                    .withSpan((Span<?>) requiredBy)
                    .withSpan((Span<?>) modifier)
                    .withDetails("incompatible value types expected: " + type + ", " + expected);
            } else {
                type = expected;
                requiredBy = modifier;
            }
        }
        return type == JelType.NONE ? null : requiredBy;
    }

    private void checkUnsupportedLiteral(
            final JelMember.Builder builder, final ContainerToken.Itr itr) throws JelException {
        final Token p = itr.peek();
        if (p == null || (p.type() != TokenType.BRACES && p.type() != TokenType.BRACKETS)) {
            return;
        }
        throw new JelException("Recursive noinline modifier support is still WIP")
            .withSpan(this.getNoInlineModifier(builder))
            .withDetails("For now, append this modifier to primitive values only");
    }

    private Span<?> getNoInlineModifier(final JelMember.Builder builder) {
        for (final Modifier modifier : builder.modifiers()) {
            if (modifier instanceof NoInlineModifier) {
                return (Span<?>) modifier;
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
