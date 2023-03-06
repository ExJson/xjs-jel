package xjs.jel.sequence;

import static xjs.jel.sequence.OperatorType.ARITHMETIC;
import static xjs.jel.sequence.OperatorType.BOOLEAN;

public enum ModifyingOperator {
    INVERT(ARITHMETIC),
    NOT(BOOLEAN);

    public final OperatorType type;

    ModifyingOperator(final OperatorType type) {
        this.type = type;
    }
}
