package xjs.jel.modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.jel.Alias;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.path.KeyComponent;
import xjs.jel.path.PathComponent;
import xjs.serialization.Span;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MergeModifierTest {

    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
        this.ctx.pushParent(Json.object());
    }

    @Test
    public void modifiedExpression_addsMembersIntoObject() throws JelException {
        final JsonObject values = Json.object().add("a", 1).add("b", 2);
        this.ctx.getScope().add("values", new JsonReference(values));

        final Modifier modifier = modifier(path(key("values")));
        final Expression modified = modifier.modify(
            Json.object().add("c", 3).add("d", 4));

        final JsonObject expected =
            Json.object().add("a", 1).add("b", 2).add("c", 3).add("d", 4);

        modified.apply(this.ctx);
        assertTrue(expected.matches(values));
    }

    @Test
    public void modifiedMember_whenAliasIsNull_generatesMembers() throws JelException {
        final JsonObject values = Json.object().add("a", 1).add("b", 2);
        this.ctx.getScope().add("values", new JsonReference(values));

        final Modifier modifier = modifier(path(key("values")));
        final JelMember member = JelMember.of((Alias) null,
            Json.object().add("a", 1).add("b", 2));

        final List<JelMember> members = modifier.modify(this.ctx, member);
        assertEquals(2, members.size());

        final JelMember one = members.get(0);
        assertEquals("a", one.getAlias().key());
        assertEquals(Json.value(1), one.getExpression().apply(this.ctx));

        final JelMember two = members.get(1);
        assertEquals("b", two.getAlias().key());
        assertEquals(Json.value(2), two.getExpression().apply(this.ctx));
    }

    @Test
    public void modifiedMember_whenValueIsNotObject_throwsException() {
        final Modifier modifier = modifier(path(key("values")));
        final JelMember member = JelMember.of((Alias) null, Json.value(1));

        assertThrows(JelException.class, () -> modifier.modify(this.ctx, member));
    }

    private static Modifier modifier(final ReferenceExpression path) {
        final MergeModifier modifier = new MergeModifier(
            new ParsedToken(TokenType.WORD, "merge"));
        modifier.captureAlias(Alias.of(path));
        return modifier;
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
