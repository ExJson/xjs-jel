package xjs.jel.sequence;

import xjs.serialization.token.Token;

public class OperatorSequence extends Sequence.Primitive {
    public final Operator op;
    public final int index;

    public OperatorSequence(final Operator op, final int index, final Token... symbols) {
        super(JelType.SYMBOL, buildList(symbols));
        this.op = op;
        this.index = index;
    }

    public OperatorSequence(
            final Operator op, final int index, final int s, final int l, final int o) {
        super(JelType.SYMBOL, buildList());
        this.op = op;
        this.index = index;
        this.start = s;
        this.end = s; // phantom
        this.line = l;
        this.offset = o;
    }
}
