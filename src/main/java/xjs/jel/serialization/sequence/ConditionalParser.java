package xjs.jel.serialization.sequence;

import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ConditionalExpression;
import xjs.jel.sequence.AliasType;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ConditionalParser extends ParserModule {

    public ConditionalParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public ConditionalExpression parse(
            final ContainerToken.Itr parentItr) throws JelException {
        final Token source = parentItr.next();
        if (source.type() != TokenType.BRACES) {
            throw new JelException("Expected conditional expression")
                .withSpan(source)
                .withDetails(
                    "Hint: a conditional expression is an object where each key is a\n"
                    + "boolean expression or '_'.\n"
                    + "For example:\n"
                    + "\n"
                    + "t >> (size) if: {\n"
                    + "  $size < 25: small\n"
                    + "  $size < 50: medium\n"
                    + "  _: large"
                    + "}\n"
                    + "r: $t(35) // == medium)");
        }
        parentItr.next();
        final ContainerToken tokens = (ContainerToken) source;
        final ContainerToken.Itr itr = tokens.iterator();

        final List<Span<?>> subs = new ArrayList<>();
        while (hasSignificantTokens(itr)) {
            this.readNextMember(subs, itr);
        }
        this.whitespaceCollector().append(subs, itr);
        return new ConditionalExpression(tokens, subs);
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
        this.keyParser().parse(builder, itr, AliasType.OPERATOR);
        this.elementParser().parse(builder, itr);
        this.whitespaceCollector().appendLineComments(builder, itr);
        spans.add(builder.build());
    }
}
