package xjs.jel.modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.jel.Alias;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.destructuring.ArrayDestructurePattern;
import xjs.jel.destructuring.DestructurePattern;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DestructureModifierTest {

    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
    }

    @Test
    public void modifyMember_appliesDestructure_fromAlias() throws JelException {
        final Modifier modifier =
            new DestructureModifier(new ParsedToken(TokenType.WORD, "from"));
        final DestructurePattern pattern = pattern(
            List.of(value("a"), value("b")), List.of());
        final JelMember member = JelMember.of(
            Alias.of(pattern), Json.array(1, 2, 3));

        final List<JelMember> modified = modifier.modify(this.ctx, member);
        assertEquals(2, modified.size());

        final JelMember one = modified.get(0);
        assertEquals("a", one.getAlias().key());
        assertEquals(Json.value(1), one.getExpression().apply(this.ctx));

        final JelMember two = modified.get(1);
        assertEquals("b", two.getAlias().key());
        assertEquals(Json.value(2), two.getExpression().apply(this.ctx));
    }

    @Test
    public void modifyMember_doesNotTolerate_nonDestructureAlias() {
        final Modifier modifier =
            new DestructureModifier(new ParsedToken(TokenType.WORD, "from"));
        final JelMember member = JelMember.of("key", Json.value("value"));

        assertThrows(JelException.class, () -> modifier.modify(this.ctx, member));
    }

    @Test
    public void modifyMember_doesNotTolerate_primitiveOutput() {
        final Modifier modifier =
            new DestructureModifier(new ParsedToken(TokenType.WORD, "from"));
        final DestructurePattern pattern = pattern(List.of(), List.of());
        final JelMember member = JelMember.of(Alias.of(pattern), Json.value(1));

        assertThrows(JelException.class, () -> modifier.modify(this.ctx, member));
    }

    @Test
    public void modifier_doesNotTolerate_directExpression() {
        final Modifier modifier =
            new DestructureModifier(new ParsedToken(TokenType.WORD, "from"));
        final Expression modified = modifier.modify(Json.value(null));

        assertThrows(JelException.class, () -> modified.apply(this.ctx));
    }

    private static ParsedToken value(final String key) {
        return new ParsedToken(TokenType.STRING, key);
    }

    private static ArrayDestructurePattern pattern(
            final List<Span<?>> beginning, final List<Span<?>> end) {
        return new ArrayDestructurePattern(
            new ContainerToken(TokenType.OPEN, List.of()),
            beginning,
            end);
    }

}
