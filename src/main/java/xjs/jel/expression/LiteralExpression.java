package xjs.jel.expression;

import org.jetbrains.annotations.Nullable;
import xjs.core.Json;
import xjs.core.JsonLiteral;
import xjs.core.JsonString;
import xjs.core.JsonType;
import xjs.core.JsonValue;
import xjs.core.StringType;
import xjs.jel.JelContext;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.Token;

import java.util.Collections;
import java.util.List;

public abstract class LiteralExpression
        extends Sequence.Primitive implements Expression {

    protected LiteralExpression(final JelType type, final @Nullable List<Token> tokens) {
        super(type, tokens != null ? tokens : Collections.emptyList());
    }

    protected LiteralExpression(final JelType type, final @Nullable Token token) {
        super(type, token != null ? Collections.singletonList(token) : Collections.emptyList());
    }

    public static LiteralExpression of(final NumberToken n) {
        return new OfNumber(n, n.number);
    }

    public static LiteralExpression of(final double n) {
        return new OfNumber(null, n);
    }

    public static LiteralExpression of(final StringToken s) {
        return new OfString(s, s.parsed());
    }

    public static LiteralExpression of(final List<Token> tokens, final String s) {
        return new OfString(tokens, s);
    }

    public static LiteralExpression of(final String s) {
        if (s == null) return new OfNull(null);
        return new OfString((StringToken) null, s);
    }

    public static LiteralExpression of(final Token t, final boolean b) {
        return new OfBoolean(t, b);
    }

    public static LiteralExpression of(final boolean b) {
        return new OfBoolean(null, b);
    }

    public static LiteralExpression of(final JsonValue value) {
        return new OfValue(null, value);
    }

    public static LiteralExpression of(final List<Token> tokens, final JsonValue value) {
        return new OfValue(tokens, value);
    }

    public static LiteralExpression ofNull(final Token t) {
        return new OfNull(t);
    }

    public static LiteralExpression ofNull() {
        return new OfNull(null);
    }

    public static class OfNumber extends LiteralExpression {
        private final double number;

        private OfNumber(final @Nullable NumberToken token, final double number) {
            super(JelType.NUMBER, token);
            this.number = number;
        }

        @Override
        public JsonValue apply(final JelContext ctx) {
            return Json.value(this.number);
        }

        @Override
        public double applyAsNumber(final JelContext ctx) {
            return this.number;
        }

        @Override
        public JsonType getStrongType() {
            return JsonType.NUMBER;
        }
    }

    public static class OfString extends LiteralExpression {
        private final String text;

        private OfString(final @Nullable StringToken token, final String text) {
            super(JelType.STRING, token);
            this.text = text;
        }

        private OfString(final List<Token> tokens, final String text) {
            super(JelType.STRING, tokens);
            this.text = text;
        }

        @Override
        public JsonValue apply(final JelContext ctx) {
            if (this.subs.isEmpty()) {
                return Json.value(this.text);
            } else if (this.subs.size() == 1) {
                final Token t = this.subs.get(0);
                if (t instanceof StringToken) {
                    return new JsonString(this.text, t.stringType());
                }
            }
            return new JsonString(this.text, StringType.IMPLICIT);
        }

        @Override
        public String applyAsString(final JelContext ctx) {
            return this.text;
        }

        @Override
        public JsonType getStrongType() {
            return JsonType.STRING;
        }
    }

    public static class OfBoolean extends LiteralExpression {
        private final boolean bool;

        private OfBoolean(final @Nullable Token token, final boolean bool) {
            super(JelType.BOOLEAN, token);
            this.bool = bool;
        }

        @Override
        public JsonValue apply(final JelContext ctx) {
            return Json.value(this.bool);
        }

        @Override
        public boolean applyAsBoolean(final JelContext ctx) {
            return this.bool;
        }

        @Override
        public JsonType getStrongType() {
            return JsonType.BOOLEAN;
        }
    }

    public static class OfValue extends LiteralExpression {
        private final JsonValue value;

        private OfValue(final @Nullable List<Token> tokens, final JsonValue value) {
            super(JelType.fromValue(value), tokens);
            this.value = value;
        }

        @Override
        public JsonValue apply(final JelContext ctx) {
            return this.value;
        }
    }

    public static class OfNull extends LiteralExpression {
        private OfNull(final @Nullable Token token) {
            super(JelType.NULL, token);
        }

        @Override
        public JsonValue apply(final JelContext ctx) {
            return JsonLiteral.jsonNull();
        }

        @Override
        public JsonType getStrongType() {
            return JsonType.NULL;
        }
    }
}
