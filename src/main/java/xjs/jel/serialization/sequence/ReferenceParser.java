package xjs.jel.serialization.sequence;


import org.jetbrains.annotations.Nullable;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.expression.TupleExpression;
import xjs.jel.path.CallComponent;
import xjs.jel.path.IndexComponent;
import xjs.jel.path.IndexRangeComponent;
import xjs.jel.path.InlinePathComponent;
import xjs.jel.path.KeyComponent;
import xjs.jel.path.PathComponent;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ReferenceParser extends ParserModule {

    public ReferenceParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public boolean isReference(
            final ContainerToken.Itr itr, final Token t, final int peek) {
        if (!t.isSymbol('$')) {
            return false;
        }
        final Token following = itr.peek(peek + 1);
        if (following == null || following.start() > t.end()) {
            return false;
        } else if (following.isSymbol('.')) {
            return true;
        }
        switch (following.type()) {
            case BREAK:
            case COMMENT:
            case SYMBOL:
            case NUMBER:
                return false;
            default:
                return true;
        }
    }

    public ReferenceExpression parse(final ContainerToken.Itr itr) throws JelException {
        return this.parse(itr, true);
    }

    public ReferenceExpression parse(
            final ContainerToken.Itr itr,
            final boolean parseSymbol) throws JelException {
        final Token first;
        if (parseSymbol) {
            first = itr.peek();
            if (first == null || !first.isSymbol('$')) {
                throw new JelException("expected reference expression")
                    .withSpan(itr.getParent());
            }
            itr.next();
        } else {
            first = itr.peek();
        }
        final List<PathComponent> components = new ArrayList<>();
        Token previous = first;
        Token peek = itr.peek();
        while (this.follows(previous, peek) || components.isEmpty() && previous == peek) {
            assert peek != null;

            if (this.isExpansion(itr, peek)) {
                itr.next();
                return ReferenceExpression.expansion(first, itr.next(), components);
            }
            final PathComponent next = this.next(itr, peek, components.isEmpty());
            if (next == null) {
                break;
            }
            components.add(next);
            previous = itr.peek(0);
            peek = itr.peek();
        }
        final Span<?> last = components.isEmpty() ? first : components.get(components.size() - 1);
        return new ReferenceExpression(first, last, components);
    }

    protected boolean isExpansion(
            final ContainerToken.Itr itr, final Token t) {
        if (!t.isSymbol('.')) {
            return false;
        }
        final Token peek = itr.peek(2);
        if (!this.follows(t, peek)) {
            return false;
        }
        assert peek != null;
        return peek.isSymbol('.');
    }

    protected PathComponent next(
            final ContainerToken.Itr itr, final Token t, final boolean first) throws JelException {
        final PathComponent keyOrCall = this.readKeyPath(itr, t, first);
        if (keyOrCall != null) {
            return keyOrCall;
        }
        final PathComponent idxOrRange = this.readIndexPath(itr, t);
        if (idxOrRange != null) {
            return idxOrRange;
        }
        final PathComponent reservedBraces = this.readReservedBraces(t);
        if (reservedBraces != null) {
            return reservedBraces;
        }
        return this.readInlinePath(itr, t);
    }

    protected @Nullable PathComponent readKeyPath(
            final ContainerToken.Itr itr, final Token t, final boolean first) throws JelException {
        if (!t.isSymbol('.') && !first) {
            return null;
        }
        final ParsedToken key;
        if (first) {
            key = this.streamAnalyzer().getKey(itr, t);
            if (key == null) {
                return null;
            }
        } else {
            itr.next();
            final Token peek = itr.peek();
            if (!this.follows(t, peek)) {
                throw new JelException("expected key")
                    .withSpan(t)
                    .withDetails("Hint: the dot accessor indicates that a word or string will follow");
            }
            assert peek != null;
            key = this.streamAnalyzer().getKey(itr, peek);
            if (key == null) {
                throw new JelException("expected key")
                    .withSpan(t)
                    .withDetails("Hint: the dot accessor indicates that a word or string will follow");
            }
        }
        itr.next();
        // todo: only one tuple -- any non-first components in parentheses are a call
        //  need infrastructure for previous components to return callables
        final List<TupleExpression> arguments =
            this.readCalls(itr, key);
        if (arguments != null) {
            return new CallComponent(key, arguments);
        }
        return new KeyComponent(key);
    }

    protected @Nullable List<TupleExpression> readCalls(
            final ContainerToken.Itr itr, final Span<?> previous) throws JelException {
        List<TupleExpression> argumentChain = null;
        TupleExpression arguments =
            this.readArguments(itr, previous);
        while (arguments != null) {
            if (argumentChain == null) {
                argumentChain = new ArrayList<>();
            }
            argumentChain.add(arguments);
            arguments = this.readArguments(itr, arguments);
        }
        return argumentChain;
    }

    protected @Nullable TupleExpression readArguments(
            final ContainerToken.Itr itr, final Span<?> previous) throws JelException {
        final Token peek = itr.peek();
        if (!this.follows(previous, peek)) {
            return null;
        }
        assert peek != null;
        if (peek.type() != TokenType.PARENTHESES) {
            return null;
        }
        itr.next();
        return this.expressionParser().tupleExpression((ContainerToken) peek, true);
    }

    protected @Nullable PathComponent readIndexPath(
            final ContainerToken.Itr itr, final Token t) throws JelException {
        if (t.type() != TokenType.BRACKETS) {
            return null;
        }
        final ContainerToken source = (ContainerToken) t;
        final ContainerToken.Itr idxItr = source.iterator();
        this.whitespaceCollector().skip(idxItr);
        NumberToken firstNumber = null;
        Token range = null;
        NumberToken secondNumber = null;
        while (idxItr.hasNext()) {
            final Token next = idxItr.next();
            if (next.type() == TokenType.NUMBER) {
                if (range == null) {
                    firstNumber = (NumberToken) next;
                } else {
                    secondNumber = (NumberToken) next;
                    break;
                }
            } else if (next.isSymbol(':')) {
                if (range != null) {
                    throw new JelException("Redundant range operator")
                        .withSpans(range, next)
                        .withDetails("Hint: expression should be [s:e], where s and e are optional numbers");
                }
                range = next;
            } else {
                throw new JelException("Unexpected tokens after index range")
                    .withSpan(next)
                    .withDetails("Hint: expression should be [s:e], where s and e are optional numbers");
            }
            this.whitespaceCollector().skip(idxItr);
        }
        if (hasSignificantTokens(idxItr)) {
            throw new JelException("Unexpected tokens after index range")
                .withSpan(itr.peek(0))
                .withDetails("Hint: expression should be [s:e], where s and e are optional numbers");
        }
        itr.next();
        if (range != null) {
            return new IndexRangeComponent(source, firstNumber, secondNumber);
        } else if (firstNumber != null) {
            return new IndexComponent(source, firstNumber);
        }
        throw new JelException("Empty index component")
            .withSpan(t)
            .withDetails("Hint: should be an index or index range component");
    }

    protected @Nullable PathComponent readReservedBraces(
            final Token t) throws JelException {
        if (t.type() != TokenType.BRACES) {
            return null;
        }
        throw new JelException("Reserved syntax is unimplemented")
            .withSpan(t)
            .withDetails("Implementation is in design. ETA release 2");
    }

    protected @Nullable PathComponent readInlinePath(
            final ContainerToken.Itr itr, final Token t) throws JelException {
        if (t.type() != TokenType.PARENTHESES) {
            return null;
        }
        final ContainerToken args = (ContainerToken) t;
        this.checkReservedLambda(args);

        final ContainerToken.Itr argItr = args.iterator();
        final JelMember member = this.elementParser().parse(argItr);
        if (argItr.hasNext()) {
            throw new JelException("Unexpected tokens at end of container")
                .withSpan(argItr.next())
                .withDetails("Inline path must contain a single JSON element");
        }
        itr.next();
        return new InlinePathComponent(args, member);
    }

    @SuppressWarnings("UnstableApiUsage")
    protected void checkReservedLambda(final ContainerToken args) throws JelException {
        final ContainerToken.Lookup lambda = args.lookup("->", true);
        if (lambda != null) {
            throw new JelException("Reserved syntax is unimplemented")
                .withSpans(lambda.token, args.get(lambda.index + 1))
                .withDetails("Implementation is in design. ETA release 2");
        }
    }

    protected boolean follows(final Span<?> t, final Span<?> peek) {
        return peek != null && peek.start() == t.end();
    }
}
