package xjs.jel.destructuring;

import org.jetbrains.annotations.Nullable;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.List;

public class KeyPattern extends Sequence.Primitive {
    public final String key;
    public final String source;

    public KeyPattern(final ParsedToken key, final @Nullable ParsedToken source) {
        super(JelType.KEY, buildList(key, source));
        this.key = key.parsed();
        this.source = source != null ? source.parsed() : this.key;
    }

    private static List<Token> buildList(
            final ParsedToken key, final @Nullable ParsedToken rename) {
        final List<Token> tokens = new ArrayList<>();
        tokens.add(key);
        if (rename != null) tokens.add(rename);
        return tokens;
    }
}
