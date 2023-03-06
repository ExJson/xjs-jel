package xjs.jel.sequence;

import xjs.core.JsonType;
import xjs.jel.expression.Expression;
import xjs.jel.expression.ReferenceExpression;
import xjs.serialization.Span;

import java.util.List;

public enum OperatorType {
    ARITHMETIC,
    RELATIONAL,
    BOOLEAN,
    OPERATOR;

    public static OperatorType fromSpans(final List<Span<?>> spans) {
        if (spans == null || spans.isEmpty()) {
            throw new IllegalArgumentException("spans");
        }
        Span<?> span = spans.get(0);
        int i = 1;
        while (!(span instanceof Expression)) {
            if (span instanceof ModifyingOperatorSequence) {
                return ((ModifyingOperatorSequence) span).op.type;
            }
            if (i >= spans.size()) {
                throw new IllegalArgumentException("no expression");
            }
            span = spans.get(i++);
        }
        return fromExpression((Expression) span);
    }

    public static OperatorType fromExpression(final Expression e) {
        if (e instanceof ReferenceExpression) {
            return ARITHMETIC; // can default to op at eval time
        } else if (e.getStrongType() == JsonType.NUMBER) {
            return ARITHMETIC;
        } else if (e.getStrongType() == JsonType.BOOLEAN) {
            return BOOLEAN;
        }
        return OPERATOR;
    }

    public boolean isCompatibleWithOperator(final OperatorType type) {
        if (this == ARITHMETIC) {
            return type == ARITHMETIC;
        }
        return true;
    }

    public boolean isCompatibleWithModifier(final OperatorType type) {
        return !(this == ARITHMETIC && type == BOOLEAN);
    }

    public boolean isBoolean() {
        return this == BOOLEAN || this == RELATIONAL;
    }
}
