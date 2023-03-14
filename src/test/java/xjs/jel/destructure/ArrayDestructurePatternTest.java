package xjs.jel.destructure;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonArray;
import xjs.core.JsonObject;
import xjs.jel.JelContext;
import xjs.jel.JelFlags;
import xjs.jel.destructuring.ArrayDestructurePattern;
import xjs.jel.destructuring.DestructurePattern;
import xjs.jel.destructuring.KeyPattern;
import xjs.jel.destructuring.ObjectDestructurePattern;
import xjs.jel.exception.JelException;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ArrayDestructurePatternTest {

    @Test
    public void destructure_copiesValuesByIndex() throws JelException {
        final JsonArray from =
            Json.array(1, 2, 3);
        final JsonObject expected = Json.object()
            .add("a", 1)
            .add("b", 2)
            .add("c", 3);
        final JsonObject into = Json.object();
        final DestructurePattern pattern = pattern(
            List.of(value("a"), value("b"), value("c")),
            List.of());

        pattern.destructure(JelContext.GLOBAL_CONTEXT, from, into);
        assertEquals(expected, into);
    }

    @Test
    public void destructure_doesNotCopy_missingValues() throws JelException {
        final JsonArray from =
            Json.array(1, 2);
        final JsonObject expected = Json.object()
            .add("a", 1)
            .add("b", 2);
        final JsonObject into = Json.object();
        final DestructurePattern pattern = pattern(
            List.of(value("a"), value("b"), value("c")),
            List.of());

        pattern.destructure(JelContext.GLOBAL_CONTEXT, from, into);
        assertEquals(expected, into);
    }

    @Test
    public void destructure_canCopyValues_fromEnd() throws JelException {
        final JsonArray from =
            Json.array(1, 2, 3, 4, 5, 6, 7);
        final JsonObject expected = Json.object()
            .add("a", 1)
            .add("b", 2)
            .add("c", 3)
            .add("x", 5)
            .add("y", 6)
            .add("z", 7);
        final JsonObject into = Json.object();
        final DestructurePattern pattern = pattern(
            List.of(value("a"), value("b"), value("c")),
            List.of(value("x"), value("y"), value("z")));

        pattern.destructure(JelContext.GLOBAL_CONTEXT, from, into);
        assertEquals(expected, into);
    }

    @Test
    public void destructure_doesNotCopyMissingValues_fromEnd() throws JelException {
        final JsonArray from =
            Json.array(1, 2);
        final JsonObject expected = Json.object()
            .add("y", 1)
            .add("z", 2);
        final JsonObject into = Json.object();
        final DestructurePattern pattern = pattern(
            List.of(),
            List.of(value("x"), value("y"), value("z")));

        pattern.destructure(JelContext.GLOBAL_CONTEXT, from, into);
        assertEquals(expected, into);
    }

    @Test
    public void destructure_toleratesNestedPatterns() throws JelException {
        final JsonArray from = Json.any(
            1,
            Json.any(2, 3),
            Json.object().add("x", 4));
        final JsonObject expected = Json.object()
            .add("a", 1)
            .add("b", 2)
            .add("c", 3)
            .add("d", 4);
        final JsonObject into = Json.object();
        final DestructurePattern pattern = pattern(
            List.of(
                value("a"),
                pattern(List.of(value("b"), value("c")), List.of()),
                pattern(key("d", "x"))),
            List.of());

        pattern.destructure(JelContext.GLOBAL_CONTEXT, from, into);
        assertEquals(expected, into);
    }

    @Test // todo; this may be unreachable
    public void destructure_doesNotCopy_privateValues() {
        final JsonArray from = Json.array()
            .add(Json.value(1).addFlag(JelFlags.PRIVATE));
        final DestructurePattern pattern = pattern(
            List.of(value("a")), List.of());

        assertThrows(JelException.class, () ->
            pattern.destructure(JelContext.GLOBAL_CONTEXT, from, Json.object()));
    }

    private static ParsedToken value(final String key) {
        return new ParsedToken(TokenType.STRING, key);
    }

    private static KeyPattern key(final String key) {
        return new KeyPattern(new ParsedToken(TokenType.STRING, key), null);
    }

    private static KeyPattern key(final String key, final String source) {
        return new KeyPattern(
            new ParsedToken(TokenType.STRING, key),
            new ParsedToken(TokenType.STRING, source));
    }

    private static ArrayDestructurePattern pattern(
            final List<Span<?>> beginning, final List<Span<?>> end) {
        return new ArrayDestructurePattern(
            new ContainerToken(TokenType.OPEN, List.of()),
            beginning,
            end);
    }

    private static ObjectDestructurePattern pattern(final KeyPattern... keys) {
        return new ObjectDestructurePattern(
            new ContainerToken(TokenType.OPEN, List.of()),
            List.of(keys));
    }
}
