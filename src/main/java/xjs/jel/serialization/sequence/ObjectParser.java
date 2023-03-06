package xjs.jel.serialization.sequence;

import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ObjectExpression;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;

import java.util.ArrayList;
import java.util.List;

public class ObjectParser extends ParserModule {

    public ObjectParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public ObjectExpression parse(final ContainerToken tokens) throws JelException {
        final ContainerToken.Itr itr = tokens.iterator();
        final List<Span<?>> spans = new ArrayList<>();

        while (hasSignificantTokens(itr)) {
            this.readNextMember(spans, itr);
        }
        this.whitespaceCollector().append(spans, itr);
        return new ObjectExpression(tokens, spans);
    }

    protected void readNextMember(
            final List<Span<?>> spans, final ContainerToken.Itr itr) throws JelException {
        final JelMember.Builder builder = JelMember.builder(JelType.MEMBER);
        if (!spans.isEmpty()) {
            this.whitespaceCollector().delimit(builder, itr);
            if (!hasSignificantTokens(itr)) {
                spans.addAll(builder.subs());
                return;
            }
        }
        this.keyParser().parse(builder, itr);
        this.elementParser().parse(builder, itr);
        this.whitespaceCollector().appendLineComments(builder, itr);
        spans.add(builder.build());
    }
}
