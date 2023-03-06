package xjs.jel.serialization.sequence;

import org.jetbrains.annotations.Nullable;
import xjs.jel.destructuring.ArrayDestructurePattern;
import xjs.jel.destructuring.DestructurePattern;
import xjs.jel.destructuring.KeyPattern;
import xjs.jel.destructuring.ObjectDestructurePattern;
import xjs.jel.exception.JelException;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class DestructureParser extends ParserModule {

    public DestructureParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public DestructurePattern parse(
            final ContainerToken tokens) throws JelException {
        if (tokens.type() == TokenType.BRACES) {
            return object(tokens.iterator());
        } else if (tokens.type() == TokenType.BRACKETS) {
            return array(tokens.iterator());
        }
        throw new JelException("Not a destructure pattern")
            .withSpan(tokens)
            .withDetails("Hint: must be an array or object pattern");
    }

    protected ObjectDestructurePattern object(
            final ContainerToken.Itr itr) throws JelException {
        final List<KeyPattern> keys = new ArrayList<>();
        this.whitespaceCollector().skip(itr);

        while (itr.hasNext()) {
            final Token next = itr.next();
            final ParsedToken key =
                this.streamAnalyzer().getKey(itr, next);
            if (key == null) {
                throw new JelException("Illegal identifier")
                    .withSpan(next)
                    .withDetails("Hint: should match the following syntax: { k: r }");
            }
            final ParsedToken source = this.getSource(itr);
            keys.add(new KeyPattern(key, source));

            this.whitespaceCollector().delimit(itr);
        }
        return new ObjectDestructurePattern(
            (ContainerToken) itr.getParent(), keys);
    }

    protected @Nullable ParsedToken getSource(
            final ContainerToken.Itr itr) throws JelException {
        final Token op = itr.peek();
        if (op == null) {
            return null;
        } else if (op.isSymbol(':')) {
            itr.next();
        } else {
            return null;
        }
        final Token source = itr.peek();
        if (source == null) {
            throw new JelException("Expected source identifier")
                .withSpan(op)
                .withDetails("Hint: should match the following syntax: { k: r }");
        }
        final ParsedToken parsed =
            this.streamAnalyzer().getKey(itr, source);
        if (parsed == null) {
            throw new JelException("Illegal source identifier")
                .withSpan(source)
                .withDetails("Hint: should match the following syntax: { k: r }");
        }
        itr.next();
        return parsed;
    }

    protected ArrayDestructurePattern array(
            final ContainerToken.Itr itr) throws JelException {
        final List<Span<?>> beginning = new ArrayList<>();
        final List<Span<?>> end = new ArrayList<>();
        this.whitespaceCollector().skip(itr);

        boolean operatorFound = false;
        while (itr.hasNext()) {
            final Token next = itr.next();
            if (next instanceof ContainerToken) {
                final DestructurePattern pattern =
                    this.parse((ContainerToken) next);
                this.add(beginning, end, operatorFound, pattern);
                this.delimitOrSeparate(itr);
                continue;
            } else if (next.isSymbol('.')) {
                final Token peek = itr.peek();
                if (peek == null || !peek.isSymbol('.')) {
                    throw new JelException("Illegal operator")
                        .withSpan(next)
                        .withDetails("Hint: should match the following pattern: [ a .. b ]");
                } else if (operatorFound) {
                    throw new JelException("Redundant operator")
                        .withSpans(next, peek)
                        .withDetails("Hint: should match the following pattern: [ a .. b ]");
                }
                operatorFound = true;
                itr.next();
                continue;
            }
            final ParsedToken key =
                this.streamAnalyzer().getKey(itr, next);
            if (key == null) {
                throw new JelException("Expected identifier, nested pattern, or '..'")
                    .withSpan(next)
                    .withDetails("Hint: should be one of: [ a, [b], {c}, .. d ]");
            }
            this.add(beginning, end, operatorFound, key);
            this.delimitOrSeparate(itr);
        }
        return new ArrayDestructurePattern(
            (ContainerToken) itr.getParent(), beginning, end);
    }

    protected void add(
            final List<Span<?>> beginning,
            final List<Span<?>> end,
            final boolean operatorFound,
            final Span<?> toAdd) {
        if (operatorFound) {
            end.add(toAdd);
        } else {
            beginning.add(toAdd);
        }
    }

    protected void delimitOrSeparate(
            final ContainerToken.Itr itr) throws JelException {
        if (!this.whitespaceCollector().tryDelimit(itr)) {
            final Token current = itr.peek();
            if (current != null && !current.isSymbol('.')) {
                throw new JelException("Expected delimiter")
                    .withSpan(current)
                    .withDetails("Hint: elements must be separated by ',' or a newline");
            }
        }
    }
}
