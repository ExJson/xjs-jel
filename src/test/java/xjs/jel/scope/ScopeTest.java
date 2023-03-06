package xjs.jel.scope;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ScopeTest {

    @Test
    public void add_thenGet_returnsValue() {
        final Scope scope = new Scope();
        scope.add("k", new JsonReference(Json.value(1)));

        assertEquals(1, scope.get("k").get().asInt());
    }

    @Test
    public void add_returnsLastValue_afterSubsequentCalls() {
        final Scope scope = new Scope();
        scope.add("k", new JsonReference(Json.value(1)));
        scope.add("k", new JsonReference(Json.value(2)));

        assertEquals(2, scope.get("k").get().asInt());
    }

    @Test
    public void add_thenDropFrame_returnsLastValue() {
        final Scope scope = new Scope();
        scope.add("k", new JsonReference(Json.value(1)));
        scope.add("k", new JsonReference(Json.value(2)));

        scope.pushFrame();
        scope.add("k", new JsonReference(Json.value(3)));
        scope.add("k", new JsonReference(Json.value(4)));
        scope.dropFrame();

        assertEquals(2, scope.get("k").get().asInt());
    }

    @Test
    public void capture_preservesReferences() {
        final Scope scope = new Scope();
        scope.add("k", new JsonReference(Json.value(1)));

        final Scope capture = scope.capture();
        assertEquals(1, capture.get("k").get().asInt());
    }

    @Test
    public void capture_preservesFrames() {
        final Scope scope = new Scope();
        scope.add("k", new JsonReference(Json.value(1)));

        scope.pushFrame();
        scope.add("k", new JsonReference(Json.value(2)));

        final Scope capture = scope.capture();
        capture.dropFrame();

        assertEquals(1, capture.get("k").get().asInt());
    }

    @Test
    public void capture_doesNotMutateOriginal() {
        final Scope scope = new Scope();
        scope.add("k", new JsonReference(Json.value(1)));

        scope.pushFrame();
        scope.add("k", new JsonReference(Json.value(2)));

        final Scope capture = scope.capture();
        capture.dropFrame();

        assertEquals(2, scope.get("k").get().asInt());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void get_allowsAccessByIndex() {
        final Scope scope = new Scope();
        scope.add(new JsonReference(Json.value(1)));
        scope.add("ignored", new JsonReference(Json.value(2)));

        assertEquals(2, scope.get(1).get().asInt());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void pushFrame_resetsIndex() {
        final Scope scope = new Scope();
        scope.add(new JsonReference(Json.value(1)));

        scope.pushFrame();
        scope.add(new JsonReference(Json.value(2)));

        assertEquals(2, scope.get(0).get().asInt());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void pushFrame_thenDropFrame_restoresIndex() {
        final Scope scope = new Scope();
        scope.add(new JsonReference(Json.value(1)));

        scope.pushFrame();
        scope.add(new JsonReference(Json.value(2)));

        scope.dropFrame();
        assertEquals(1, scope.get(0).get().asInt());
    }
}
