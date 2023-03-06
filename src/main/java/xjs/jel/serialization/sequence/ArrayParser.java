package xjs.jel.serialization.sequence;

import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ArrayExpression;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;

import java.util.ArrayList;
import java.util.List;

public class ArrayParser extends ParserModule {

    public ArrayParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public ArrayExpression parse(final ContainerToken tokens) throws JelException {
        final ContainerToken.Itr itr = tokens.iterator();
        final List<Span<?>> spans = new ArrayList<>();

        while (hasSignificantTokens(itr)) {
            this.readNextElement(spans, itr);
        }
        this.whitespaceCollector().append(spans, itr);
        return new ArrayExpression(tokens, spans);
    }

    protected void readNextElement(
            final List<Span<?>> spans, final ContainerToken.Itr itr) throws JelException {
        final JelMember.Builder builder = JelMember.builder(JelType.ELEMENT);
        if (!spans.isEmpty()) {
            this.whitespaceCollector().delimit(builder, itr);
            if (!hasSignificantTokens(itr)) {
                spans.addAll(builder.subs());
                return;
            }
        }
        this.elementParser().parse(builder, itr);
        this.whitespaceCollector().appendLineComments(builder, itr);
        spans.add(builder.build());
    }
}
