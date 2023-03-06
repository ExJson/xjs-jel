package xjs.jel.destructure;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonObject;
import xjs.jel.JelFlags;
import xjs.jel.destructuring.DestructurePattern;
import xjs.jel.destructuring.KeyPattern;
import xjs.jel.destructuring.ObjectDestructurePattern;
import xjs.jel.exception.JelException;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ObjectDestructurePatternTest {

    @Test
    public void destructure_copiesValuesByKey() throws JelException {
        final JsonObject from = Json.object()
            .add("a", 1)
            .add("b", 2)
            .add("c", 3);
        final JsonObject expected = Json.object()
            .add("a", 1)
            .add("c", 3);
        final JsonObject into = Json.object();
        final DestructurePattern pattern =
            pattern(key("a"), key("c"));

        pattern.destructure(from, into);
        assertEquals(expected, into);
    }

    @Test
    public void destructure_canRenameKeys() throws JelException {
        final JsonObject from = Json.object()
            .add("a", 1)
            .add("b", 2)
            .add("c", 3);
        final JsonObject expected = Json.object()
            .add("x", 1)
            .add("y", 2)
            .add("z", 3);
        final JsonObject into = Json.object();
        final DestructurePattern pattern =
            pattern(key("x", "a"), key("y", "b"), key("z", "c"));

        pattern.destructure(from, into);
        assertEquals(expected, into);
    }

    @Test // todo: this may be unreachable
    public void destructure_doesNotCopy_privateKeys() {
        final JsonObject from = Json.object()
            .add("a", Json.value(1).addFlag(JelFlags.PRIVATE));
        final DestructurePattern pattern =
            pattern(key("a"));

        assertThrows(JelException.class, () ->
            pattern.destructure(from, Json.object()));
    }

    private static KeyPattern key(final String key) {
        return new KeyPattern(new ParsedToken(TokenType.STRING, key), null);
    }

    private static KeyPattern key(final String key, final String source) {
        return new KeyPattern(
            new ParsedToken(TokenType.STRING, key),
            new ParsedToken(TokenType.STRING, source));
    }

    private static ObjectDestructurePattern pattern(final KeyPattern... keys) {
        return new ObjectDestructurePattern(
            new ContainerToken(TokenType.OPEN, List.of()),
            List.of(keys));
    }
}
