package xjs.jel.serialization.sequence;

import org.jetbrains.annotations.Nullable;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;

public class ParserModule {
    private final Sequencer sequencer;

    protected ParserModule(final Sequencer sequencer) {
        this.sequencer = sequencer;
    }

    protected final ArrayParser arrayParser() {
        return this.sequencer.arrayParser;
    }

    protected final ConditionalParser conditionalParser() {
        return this.sequencer.conditionalParser;
    }

    protected final DestructureParser destructureParser() {
        return this.sequencer.destructureParser;
    }

    protected final ElementParser elementParser() {
        return this.sequencer.elementParser;
    }

    protected final KeyParser keyParser() {
        return this.sequencer.keyParser;
    }

    protected final MatchParser matchParser() {
        return this.sequencer.matchParser;
    }

    protected final ModifierParser modifierParser() {
        return this.sequencer.modifierParser;
    }

    protected final ObjectParser objectParser() {
        return this.sequencer.objectParser;
    }

    protected final OperatorParser operatorParser() {
        return this.sequencer.operatorParser;
    }

    protected final ReferenceParser referenceParser() {
        return this.sequencer.referenceParser;
    }

    protected final SimpleExpressionParser expressionParser() {
        return this.sequencer.expressionParser;
    }

    protected final StreamAnalyzer streamAnalyzer() {
        return this.sequencer.streamAnalyzer;
    }

    protected final ValueParser valueParser() {
        return this.sequencer.valueParser;
    }

    protected final WhitespaceCollector whitespaceCollector() {
        return this.sequencer.whitespaceCollector;
    }

    protected static boolean hasSignificantTokens(final ContainerToken.Itr itr) {
        Token peek = itr.peek();
        int amount = 0;
        while (peek != null) {
            if (JelType.isSignificant(peek)) {
                return true;
            }
            peek = itr.peek(++amount);
        }
        return false;
    }

    protected static boolean isFollowedBy(
            final Token t, final ContainerToken.Itr itr, final int peekAmount, final char expected) {
        return isFollowedBy(t, itr.peek(peekAmount + 1), expected);
    }

    protected static boolean isFollowedBy(
            final Token t, final ContainerToken.Itr itr, final char expected) {
        if (isFollowedBy(t, itr.peek(), expected)) {
            itr.next();
            return true;
        }
        return false;
    }

    protected static boolean isFollowedBy(
            final Span<?> t, final @Nullable Token by, final char expected) {
        return by != null && by.start() == t.end() && by.isSymbol(expected);
    }

    public static int getNextGap(final ContainerToken.Itr itr, final int e) {
        return getNextGap(itr, 1, e);
    }

    public static int getNextGap(final ContainerToken.Itr itr, int peekAmount, final int e) {
        final int idx = itr.getIndex();
        Token peek = itr.peek(peekAmount);

        while (peek != null) {
            final Token following = itr.peek(++peekAmount);
            if (following == null) {
                return e;
            } else if (following.start() > peek.end()) {
                return idx + peekAmount - 1;
            }
            peek = following;
        }
        return e;
    }

    public static int nextSignificant(final ContainerToken.Itr itr) {
        return nextSignificant(itr, ((ContainerToken) itr.getParent()).size());
    }

    public static int nextSignificant(final ContainerToken.Itr itr, final int e) {
        final int idx = itr.getIndex();
        Token peek = itr.peek();
        int peekAmount = 1;
        while (peek != null && idx + peekAmount <= e) {
            if (JelType.isSignificant(peek)) {
                return itr.getIndex() + peekAmount - 1;
            }
            peek = itr.peek(++peekAmount);
        }
        return e;
    }

    public static int skipBackwards(final ContainerToken.Itr itr, final int e) {
        final ContainerToken parent = (ContainerToken) itr.getParent();
        for (int i = e - 1; i >= 0; i--) {
            if (JelType.isSignificant(parent.get(i))) {
                return i + 1;
            }
        }
        return 0;
    }
}
