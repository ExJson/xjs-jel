package xjs.jel.path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonContainer;
import xjs.core.JsonLiteral;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.Privilege;
import xjs.jel.expression.LiteralExpression;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonPathTest {

    @BeforeAll
    public static void init() {
        JelContext.GLOBAL_CONTEXT.setPrivilege(Privilege.ALL);
    }

    @AfterAll
    public static void close() {
        JelContext.GLOBAL_CONTEXT.setPrivilege(Privilege.BASIC);
    }

    @Test
    public void get_withSingleKey_getsValueFromContainer() {
        final String key = "key";
        final JsonValue value = Json.value("value");
        final JsonContainer c = Json.object()
            .add(key, value);
        final JsonPath path = path(key(key));

        assertEquals(value, path.get(c));
    }

    @Test
    public void get_withMultipleKeys_getsValueFromContainer() {
        final String k1 = "k1";
        final String k2 = "k2";
        final JsonValue value = Json.value("value");
        final JsonContainer c = Json.object()
            .add(k1, Json.object()
                .add(k2, value));
        final JsonPath path = path(key(k1), key(k2));

        assertEquals(value, path.get(c));
    }

    @Test
    public void get_withIndex_getsValueFromContainer() {
        final JsonValue value = Json.value(1234);
        final JsonContainer c = Json.array()
            .add("ignored")
            .add("ignored")
            .add(value);
        final JsonPath path = path(index(2));

        assertEquals(value, path.get(c));
    }

    @Test
    public void get_withIndex_canSubstituteKey() {
        final JsonValue value = Json.value(1234);
        final JsonContainer c = Json.object()
            .add("ignored 1", 0)
            .add("ignored 2", 0)
            .add("ignored 3", value);
        final JsonPath path = path(index(2));

        assertEquals(value, path.get(c));
    }

    @Test
    public void get_withNegativeIndex_getsFromEnd() {
        final JsonValue value = Json.value(1234);
        final JsonContainer c = Json.array()
            .add("ignored")
            .add("ignored")
            .add("ignored")
            .add("ignored")
            .add(value)
            .add("ignored");
        final JsonPath path = path(index(-2));

        assertEquals(value, path.get(c));
    }

    @Test
    public void get_withMultipleIndices_getsValueFromContainer() {
        final JsonValue value = Json.value(1234);
        final JsonContainer c = Json.array()
            .add("ignored")
            .add("ignored")
            .add(Json.array()
                .add(value));
        final JsonPath path = path(index(2), index(0));

        assertEquals(value, path.get(c));
    }

    @Test
    public void get_withIndexRange_getsMultipleValues() {
        final JsonContainer c = Json.array(1, 2, 3, 4, 5);
        final JsonPath path = path(range(1, 3));

        assertTrue(Json.array(2, 3, 4).matches(path.get(c)));
    }

    @Test
    public void get_withIndexRange_canSubstituteKeys() {
        final JsonContainer c = Json.object()
            .add("ignored 1", 1)
            .add("ignored 2", 2)
            .add("ignored 3", 3)
            .add("ignored 4", 4)
            .add("ignored 5", 5);
        final JsonPath path = path(range(2, 4));

        assertTrue(Json.array(3, 4, 5).matches(path.get(c)));
    }

    @Test
    public void get_withCallComponent_returnsValueOfExpression() {
        final JsonContainer c = Json.object()
            .add("key", "value");
        final JsonPath path = path(
            key("key"),
            call("startsWith", Json.value("val")));

        assertTrue(path.get(c).isTrue());
    }

    @Test
    public void get_toleratesMultipleCallComponents() {
        final JsonContainer c = Json.object()
            .add("values", Json.array()
                .add(Json.object(Map.of("id", 1, "value", "abc")))
                .add(Json.object(Map.of("id", 2, "value", "def")))
                .add(Json.object(Map.of("id", 3, "value", "ghi"))));
        final JsonPath path = path(
            key("values"),
            call("find", Json.object().add("id", 2)),
            key("value"),
            call("startsWith", Json.value("d")));

        assertTrue(path.get(c).isTrue());
    }

    @Test
    public void get_toleratesChainedCallComponents() {
        final JsonContainer c = Json.object()
            .add("key", "value");
        final JsonPath path = path(
            key("key"),
            call("startsWith", Json.value("val")),
            call("number"),
            call("type"),
            call("contains", Json.value("number")));

        assertTrue(path.get(c).isTrue());
    }

    @Test
    public void get_withInlineComponent_allowsDynamicPathing() {
        final JsonPath path = path(
            inline(Json.object().add("a", 1)),
            key("a"));

        assertEquals(1, path.get(new JsonObject()).asInt());
    }

    @Test
    public void get_withMultipleTypes_getsValueFromContainer() {
        final String key = "key";
        final JsonValue value = Json.value(1234);
        final JsonContainer c = Json.array()
            .add("ignored")
            .add("ignored")
            .add(Json.object()
                .add("ignored", "ignored")
                .add(key, value));
        final JsonPath path =
            path(index(2), key(key));

        assertEquals(value, path.get(c));
    }

    @Test
    public void get_whenPathDoesNotExist_returnsJsonNull() {
        final JsonContainer c = Json.object()
            .add("a", 1)
            .add("b", Json.array(2, 3, 4))
            .add("c", 5);
        final JsonPath path = path(key("b"), index(3));
        assertEquals(JsonLiteral.jsonNull(), path.get(c));
    }

    @Test
    public void getAll_whenPathDoesNotExist_returnsEmpty() {
        final JsonContainer c = Json.object()
            .add("a", 1)
            .add("b", Json.array(2, 3, 4))
            .add("c", 5);
        final JsonPath path = path(key("b"), index(3));
        assertEquals(List.of(), path.getAll(c));
    }

    private static JsonPath path(final PathComponent... components) {
        return JsonPath.of(Arrays.asList(components));
    }

    private static KeyComponent key(final String key) {
        return new KeyComponent(new ParsedToken(TokenType.WORD, key));
    }

    private static IndexComponent index(final int index) {
        return new IndexComponent(
            new ContainerToken(TokenType.OPEN, List.of()),
            new NumberToken(index));
    }

    private static IndexRangeComponent range(final int s, final int e) {
        return new IndexRangeComponent(
            new ContainerToken(TokenType.OPEN, List.of()),
            new NumberToken(s),
            new NumberToken(e));
    }

    private static InlinePathComponent inline(final JsonValue value) {
        return new InlinePathComponent(
            new ContainerToken(TokenType.OPEN, List.of()),
            LiteralExpression.of(value));
    }

    private static CallComponent call(final String key, final JsonValue... args) {
        return new CallComponent(
            new ParsedToken(TokenType.WORD, key),
            List.of(Json.any((Object[]) args)),
            List.of());
    }
}
