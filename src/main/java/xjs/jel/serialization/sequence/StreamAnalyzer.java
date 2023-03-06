package xjs.jel.serialization.sequence;

import org.jetbrains.annotations.Nullable;
import xjs.jel.exception.JelException;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class StreamAnalyzer extends ParserModule {

    public StreamAnalyzer(final Sequencer sequencer) {
        super(sequencer);
    }

    public @Nullable ParsedToken getKey(
            final ContainerToken.Itr itr, final Token t) {
        if (t.type() == TokenType.STRING) {
            return (ParsedToken) t;
        } else if (t.type() == TokenType.WORD) {
            return t.intoParsed(itr.getReference());
        }
        return null;
    }

    public List<ParsedToken> getKeys(final ContainerToken.Itr itr) throws JelException {
        final List<ParsedToken> params = new ArrayList<>();
        this.whitespaceCollector().skip(itr);

        while (itr.hasNext()) {
            final Token next = itr.next();
            final ParsedToken key = this.getKey(itr, next);
            if (key == null) {
                throw new JelException("Expected a key")
                    .withSpan(next)
                    .withDetails("Hint: should match the following pattern: (a, b, c)");
            }
            params.add(key);
            this.whitespaceCollector().delimit(itr);
        }
        return params;
    }

    public void checkDangling(
            final ContainerToken.Itr itr, final int e) throws JelException {
        if (itr.getIndex() < e) {
            throw new JelException("illegal dangling tokens after expression")
                .withSpan(itr.peek())
                .withDetails("Hint: objects and arrays may not have trailing tokens\n"
                    + "Hint: some modifiers expect single-token values");
        }
    }
}
