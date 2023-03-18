package xjs.jel.sequence;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonValue;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

public enum JelType {
    ROOT,
    ELEMENT,
    NUMBER,
    NUMBER_EXPRESSION,
    BOOLEAN,
    BOOLEAN_EXPRESSION,
    NULL,
    STRING,
    STRING_EXPRESSION,
    SYMBOL,
    COMMENTS,
    NEWLINES,
    ARRAY,
    INDEX,
    INDEX_RANGE,
    OBJECT,
    MEMBER,
    KEY,
    ALIAS,
    SCOPE,
    ARRAY_PATTERN,
    OBJECT_PATTERN,
    FLAG,
    ARRAY_GENERATOR,
    OBJECT_GENERATOR,
    IDENTIFIERS,
    IMPORT,
    TUPLE,
    MATCH,
    DELEGATE,
    CONDITIONAL,
    CONDITIONAL_BRANCH,
    TEMPLATE,
    CALL,
    REFERENCE,
    REFERENCE_EXPANSION,
    NONE;

    JelType() {
        this(null);
    }

    JelType(final @Nullable JelType parent) {
        this.parent = parent;
    }

    private final @Nullable JelType parent;

    public boolean isParent(final JelType type) {
        if (this.parent != null) {
            return this.parent.is(type);
        }
        return false;
    }

    public boolean is(final JelType type) {
        return this == type || this.isParent(type);
    }

    public boolean isSignificant() {
        return this != COMMENTS && this != NEWLINES;
    }

    public static JelType fromValue(final JsonValue value) {
        switch (value.getType()) {
            case STRING: return STRING;
            case NUMBER: return NUMBER;
            case BOOLEAN: return BOOLEAN;
            case ARRAY: return ARRAY;
            case OBJECT: return OBJECT;
            default: return NULL;
        }
    }

    public static boolean isSignificant(final Object o) {
        if (o instanceof Sequence<?>) {
            return ((Sequence<?>) o).type().isSignificant();
        } else if (o instanceof Token) {
            final TokenType t = ((Token) o).type();
            return t != TokenType.COMMENT && t != TokenType.BREAK;
        }
        throw new IllegalStateException("unknown type: " + o);
    }
}
