package xjs.jel.serialization.sequence;

import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.MatchExpression;
import xjs.jel.modifier.MatchModifier;
import xjs.jel.modifier.Modifier;
import xjs.jel.sequence.AliasType;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class MatchParser extends ParserModule {

    public MatchParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public MatchExpression parse(
            final ContainerToken.Itr parentItr, final Modifier modifier) throws JelException {
        if (!(modifier instanceof MatchModifier)) {
            throw new IllegalArgumentException("unexpected modifier passed");
        }
        final MatchModifier match = (MatchModifier) modifier;
        final Token source = parentItr.next();
        if (source.type() != TokenType.BRACES) {
            throw new JelException("Expected match expression")
                .withSpan(source)
                .withDetails(
                    "Hint: a match expression is an object where each key may be any value or '_'\n"
                    + "For example:\n"
                    + "\n"
                    + "color: green\n"
                    + "r >> match $color: { red: 1, green: 2, blue: 3, _: 4 }\n"
                    + "// r: 4");
        }
        parentItr.next();
        final ContainerToken tokens = (ContainerToken) source;
        final ContainerToken.Itr itr = tokens.iterator();

        final List<Span<?>> subs = new ArrayList<>();
        while (hasSignificantTokens(itr)) {
            this.readNextMember(subs, itr);
        }
        this.whitespaceCollector().append(subs, itr);
        return new MatchExpression(match.ref, tokens, subs);
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
        this.keyParser().parse(builder, itr, AliasType.VALUE);
        this.elementParser().parse(builder, itr);
        this.whitespaceCollector().appendLineComments(builder, itr);
        spans.add(builder.build());
    }
}
