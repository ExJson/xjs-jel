package xjs.jel;

import xjs.jel.destructuring.DestructurePattern;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.expression.OperatorExpression;
import xjs.jel.path.JsonPath;
import xjs.jel.sequence.AliasType;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Alias extends Sequence.Parent {
    private final Object alias;

    private Alias(final Sequence<?> sequence, final Object alias) {
        super(JelType.ALIAS, buildList(sequence));
        this.alias = alias;
    }

    public static Alias of(final String key) {
        return of(LiteralExpression.of(key));
    }

    public static Alias of(final LiteralExpression key) {
        try {
            return new Alias(key, key.applyAsString(null));
        } catch (final JelException ignored) {
            throw new IllegalStateException("expected key");
        }
    }

    public static Alias of(final DestructurePattern pattern) {
        return new Alias(pattern, pattern);
    }

    public static Alias of(final Expression value) {
        return new Alias((Sequence<?>) value, value);
    }

    public String key() {
        if (this.alias instanceof String) {
            return (String) this.alias;
        }
        throw new UnsupportedOperationException("not a key");
    }

    public JsonPath path() {
        if (this.alias instanceof JsonPath) {
            return (JsonPath) this.alias;
        }
        throw new UnsupportedOperationException("not a path");
    }

    public OperatorExpression operator() {
        if (this.alias instanceof OperatorExpression) {
            return (OperatorExpression) this.alias;
        }
        throw new UnsupportedOperationException("not an operator expression");
    }

    public Expression value() {
        if (this.alias instanceof Expression) {
            return (Expression) this.alias;
        }
        throw new UnsupportedOperationException("not an expression");
    }

    public DestructurePattern pattern() {
        if (this.alias instanceof DestructurePattern) {
            return (DestructurePattern) this.alias;
        }
        throw new UnsupportedOperationException("not a pattern");
    }

    public AliasType aliasType() {
        if (this.alias instanceof String) {
            return AliasType.LITERAL;
        } else if (this.alias instanceof JsonPath) {
            return AliasType.REFERENCE;
        } else if (this.alias instanceof OperatorExpression) {
            return AliasType.OPERATOR;
        } else if (this.alias instanceof Expression) {
            return AliasType.VALUE;
        }
        return AliasType.DESTRUCTURE;
    }

    @Override
    public List<Span<?>> flatten() {
        if (this.alias instanceof String) {
            return Collections.singletonList(
                new Sequence.Parent(JelType.KEY, Collections.singletonList(this)));
        }
        return super.flatten();
    }
}
