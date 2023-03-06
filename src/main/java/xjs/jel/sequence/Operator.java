package xjs.jel.sequence;

import static xjs.jel.sequence.OperatorType.ARITHMETIC;
import static xjs.jel.sequence.OperatorType.BOOLEAN;
import static xjs.jel.sequence.OperatorType.RELATIONAL;

public enum Operator {
    ADD(ARITHMETIC),
    SUBTRACT(ARITHMETIC),
    MULTIPLY(ARITHMETIC),
    DIVIDE(ARITHMETIC),
    MOD(ARITHMETIC),
    POW(ARITHMETIC),
    BITWISE_AND(ARITHMETIC),
    BITWISE_OR(ARITHMETIC),
    LEFT_SHIFT(ARITHMETIC),
    RIGHT_SHIFT(ARITHMETIC),
    AND(BOOLEAN),
    OR(BOOLEAN),
    GREATER_THAN(RELATIONAL),
    GREATER_THAN_EQUAL_TO(RELATIONAL),
    LESS_THAN(RELATIONAL),
    LESS_THAN_EQUAL_TO(RELATIONAL),
    EQUAL_TO(RELATIONAL),
    NOT_EQUAL_TO(RELATIONAL);

    public final OperatorType type;

    Operator(final OperatorType type) {
        this.type = type;
    }

    public boolean isPureMath() {
        if (this.type != ARITHMETIC) {
            return false;
        }
        switch (this) {
            case ADD:
            case SUBTRACT:
            case MULTIPLY:
                return false;
            default:
                return true;
        }
    }
}
