package xjs.jel.sequence;

import xjs.serialization.token.Token;

public class ModifyingOperatorSequence extends Sequence.Primitive {
    public final ModifyingOperator op;

    public ModifyingOperatorSequence(final ModifyingOperator op, final Token symbol) {
        super(JelType.SYMBOL, buildList(symbol));
        this.op = op;
    }
}
