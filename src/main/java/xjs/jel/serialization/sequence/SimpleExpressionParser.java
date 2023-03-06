package xjs.jel.serialization.sequence;

import org.jetbrains.annotations.Nullable;
import xjs.core.StringType;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.expression.StringExpression;
import xjs.jel.expression.TupleExpression;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleExpressionParser extends ParserModule {

    public SimpleExpressionParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public boolean isStringExpression(
            final ContainerToken.Itr itr, final int e) {
        final int idx = itr.getIndex();
        Token peek = itr.peek();
        int peekAmount = 1;
        while (peek != null && idx + peekAmount <= e) {
            if (peek instanceof ContainerToken) {
                final ContainerToken tokens = (ContainerToken) peek;
                if (this.isStringExpression(tokens.iterator(), tokens.size())) {
                    return true;
                }
            } else if (this.referenceParser().isReference(itr, peek, peekAmount)) {
                return true;
            }
            peek = itr.peek(++peekAmount);
        }
        return false;
    }

    public StringExpression stringExpression(
            final ContainerToken.Itr itr, final int e) throws JelException {
        if (itr.getIndex() == e) {
            return new StringExpression(Collections.emptyList());
        }
        final Token first = itr.peek();
        assert first != null;
        final List<Span<?>> spans = new ArrayList<>();
        final int o = first.offset();
        int prevIdx = first.start();
        int prevLine = first.line();
        for (final Span<?> span : this.getRefsAndImplicit(itr, e)) {
            if (span.start() > prevIdx) {
                final String text = this.buildImplicitText(itr, prevIdx, o, span.start());
                spans.add(new StringToken(
                    prevIdx, span.start(), prevLine, prevIdx, StringType.IMPLICIT, text));
            }
            spans.add(span);
            prevIdx = span.end();
            prevLine = span.lastLine();
        }
        final Token last = itr.peek(0);
        assert last != null;
        if (prevIdx < last.end()) {
            final String text = this.buildImplicitText(itr, prevIdx, o, last.end());
            spans.add(new StringToken(
                prevIdx, last.end(), prevLine, prevIdx, StringType.IMPLICIT, text));
        }
        return new StringExpression(spans);
    }

    protected List<Span<?>> getRefsAndImplicit(
            final ContainerToken.Itr itr, final int e) throws JelException {
        final List<Span<?>> spans = new ArrayList<>();
        Token peek = itr.peek();
        while (peek != null && itr.getIndex() < e) {
            if (peek instanceof StringToken && peek.stringType() == StringType.IMPLICIT) {
                spans.add(peek);
                itr.next();
            } else if (peek instanceof ContainerToken) {
                final ContainerToken tokens = (ContainerToken) peek;
                spans.addAll(this.getRefsAndImplicit(tokens.iterator(), tokens.size()));
                itr.next();
            } else if (this.referenceParser().isReference(itr, peek, 1)) {
                spans.add(this.referenceParser().parse(itr));
            } else {
                itr.next();
            }
            peek = itr.peek();
        }
        return spans;
    }

    public TupleExpression tupleExpression(
            final ContainerToken tokens) throws JelException {
        return this.tupleExpression(tokens, false);
    }

    public TupleExpression tupleExpression(
            final ContainerToken tokens, final boolean inlined) throws JelException {
        final ContainerToken.Itr itr = tokens.iterator();
        final List<Span<?>> spans = new ArrayList<>();

        while (hasSignificantTokens(itr)) {
            this.readNextArgument(spans, itr, inlined);
        }
        this.whitespaceCollector().append(spans, itr);
        return new TupleExpression(tokens, spans);
    }

    protected void readNextArgument(
            final List<Span<?>> spans, final ContainerToken.Itr itr,
            final boolean inlined) throws JelException {
        final JelMember.Builder builder = JelMember.builder(JelType.ELEMENT);
        if (!spans.isEmpty()) {
            this.whitespaceCollector().delimit(builder, itr);
        }
        if (inlined) {
            spans.add((Span<?>) this.elementParser().parseInline(itr));
        } else {
            this.elementParser().parse(builder, itr);
            this.whitespaceCollector().appendLineComments(builder, itr);
            spans.add(builder.build());
        }
    }

    public LiteralExpression literalPrimitive(
            final ContainerToken.Itr itr, final int e) {
        final Token first = itr.peek();
        if (first == null) {
            return this.voidString(itr.getParent());
        } else if (e == 0 || itr.getIndex() == e) {
            return this.voidString(first);
        }
        final Token last = ((ContainerToken) itr.getParent()).get(e - 1);

        if (first == last) {
            final LiteralExpression single = this.tryLiteral(first);
            if (single != null) {
                itr.skipTo(e);
                return single;
            }
        }
        final List<Token> tokens =
            itr.getParent().viewTokens().subList(itr.getIndex(), e);
        itr.skipTo(e);
        return this.expressionOf(tokens, this.getText(itr, first, last));
    }

    protected LiteralExpression voidString(final Span<?> span) {
        final int s = span.start();
        final int l = span.line();
        final int o = span.offset();
        return LiteralExpression.of(
            new StringToken(s, s, l, o, StringType.IMPLICIT, ""));
    }

    protected @Nullable LiteralExpression tryLiteral(final Token t) {
        if (t instanceof NumberToken) {
            return LiteralExpression.of((NumberToken) t);
        } else if (t instanceof StringToken) {
            return LiteralExpression.of((StringToken) t);
        }
        return null;
    }

    protected String getText(
            final ContainerToken.Itr itr, final Span<?> first, final Span<?> last) {
        return this.getText(itr, first.offset(), first, last);
    }

    protected String getText(
            final ContainerToken.Itr itr, final int o,
            final Span<?> first, final Span<?> last) {
        if (first.line() == last.lastLine()) {
            return itr.getText(first.start(), last.end());
        } else {
            return this.buildImplicitText(
                itr, first.start(), o, last.end());
        }
    }

    protected String buildImplicitText(
            final ContainerToken.Itr itr, final int s, final int o, final int e) {
        final StringBuilder sb = new StringBuilder();
        final CharSequence ref = itr.getReference();
        int marker = s;
        for (int i = s; i < e; i++) {
            char c = ref.charAt(i);
            if (c == '\n') {
                sb.append(ref, marker, i + 1);
                i = this.getActualOffset(ref, i + 1, o);
                marker = i;
            } else if (c == '\\' && i < e - 1) {
                sb.append(ref, marker, i);
                c = ref.charAt(++i);
                if (c != '\r' && c != '\n') {
                    sb.append('\\');
                }
                sb.append(c);
                marker = i;
            }
        }
        sb.append(ref, marker, e);
        return sb.toString();
    }

    protected int getActualOffset(
            final CharSequence ref, final int s, final int o) {
        for (int i = s; i < s + o; i++) {
            if (!this.isLineWhitespace(ref.charAt(i))) {
                return i;
            }
        }
        return s + o;
    }

    protected boolean isLineWhitespace(final char c) {
        return c == ' ' || c == '\r' || c == '\t';
    }

    protected LiteralExpression expressionOf(final List<Token> tokens, final String text) {
        switch (text) {
            case "true": return LiteralExpression.of(tokens.get(0), true);
            case "false": return LiteralExpression.of(tokens.get(0), false);
            case "null": return LiteralExpression.ofNull(tokens.get(0));
        }
        return LiteralExpression.of(tokens, text);
    }
}
