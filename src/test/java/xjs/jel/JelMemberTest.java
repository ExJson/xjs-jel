package xjs.jel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonArray;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.modifier.AddModifier;
import xjs.jel.modifier.ImportModifier;
import xjs.jel.modifier.MergeModifier;
import xjs.jel.modifier.Modifier;
import xjs.jel.path.KeyComponent;
import xjs.jel.path.PathComponent;
import xjs.serialization.Span;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JelMemberTest {

    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
        this.ctx.pushParent(Json.object());
    }

    @Test
    public void process_appliesSimpleModifier() throws JelException {
        final JsonArray values = new JsonArray();
        this.ctx.getScope().add("values", new JsonReference(values));

        final ReferenceExpression path = path(key("values"));
        final Modifier modifier = addModifier(path);
        final JelMember member = JelMember.of(
            Alias.of(path), List.of(modifier), Json.value("value"));

        member.process(this.ctx);
        member.getExpression().apply(this.ctx);

        assertTrue(Json.array("value").matches(values));
    }

    @Test
    public void process_whenModifiersIsEmpty_returnsSelf() throws JelException {
        final JsonValue value = Json.value("value");
        final JelMember member = JelMember.of("key", value);

        assertSame(member, member.process(this.ctx).get(0));
        assertSame(value, member.getExpression().apply(this.ctx));
    }

    @Test
    public void process_returnsAllValues_fromModifier() throws JelException {
        final JsonObject object = Json.object().add("a", 1).add("b", 2);
        this.ctx.getScope().add("values", new JsonReference(object));

        final ReferenceExpression path = path(key("values"));
        final Modifier modifier = mergeModifier(path);
        final JelMember member = JelMember.of(null, List.of(modifier), object);

        final List<JelMember> members = member.process(this.ctx);
        assertEquals(2, members.size());

        final JelMember one = members.get(0);
        assertEquals("a", one.getAlias().key());
        assertEquals(Json.value(1), one.getExpression().apply(this.ctx));

        final JelMember two = members.get(1);
        assertEquals("b", two.getAlias().key());
        assertEquals(Json.value(2), two.getExpression().apply(this.ctx));
    }

    @Test
    public void process_appliesSubsequentModifiers_toAllValues() throws JelException {
        final JsonArray array = new JsonArray();
        this.ctx.getScope().add("values", new JsonReference(array));

        final ReferenceExpression path = path(key("values"));
        final List<Modifier> modifiers =
            List.of(importModifier(), addModifier(path));
        final JelMember member = JelMember.of(
            Alias.of(path), modifiers, Json.array("a.xjs", "b.xjs"));

        this.ctx.addOutput(new File("/a.xjs"), Json.value(1));
        this.ctx.addOutput(new File("/b.xjs"), Json.value(2));

        final List<JelMember> members = member.process(this.ctx);
        assertEquals(0, members.size());

        assertTrue(Json.array(1, 2).matches(array));
    }

    private static Modifier addModifier(final ReferenceExpression path) {
        final AddModifier modifier = new AddModifier(
            new ParsedToken(TokenType.WORD, "add"));
        modifier.captureAlias(Alias.of(path));
        return modifier;
    }

    private static Modifier mergeModifier(final ReferenceExpression path) {
        final MergeModifier modifier = new MergeModifier(
            new ParsedToken(TokenType.WORD, "merge"));
        modifier.captureAlias(Alias.of(path));
        return modifier;
    }

    private static Modifier importModifier() {
        return new ImportModifier(
            new ParsedToken(TokenType.WORD, "import"));
    }

    private static ReferenceExpression path(final PathComponent... components) {
        final Span<?> first = components[0];
        final Span<?> last = components[components.length - 1];
        return new ReferenceExpression(first, last, Arrays.asList(components));
    }

    private static KeyComponent key(final String key) {
        return new KeyComponent(new ParsedToken(TokenType.WORD, key));
    }
}
