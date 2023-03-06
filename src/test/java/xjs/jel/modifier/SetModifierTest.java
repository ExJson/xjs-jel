package xjs.jel.modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SetModifierTest {

    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
        this.ctx.pushParent(Json.object());
    }

    @Test
    public void modifiedExpression_setsValueByAlias() throws JelException {
        final JsonReference ref = new JsonReference(Json.value(1));
        this.ctx.getScope().add("values", ref);

        final Modifier modifier = modifier(path(key("values")));
        final JsonValue updated = Json.value(2);
        final Expression modified = modifier.modify(updated);

        modified.apply(this.ctx);
        assertSame(updated, ref.get());
    }

    @Test // not yet supported
    public void modifiedMember_whenVariableIsUndefined_throwsException() {
        final ReferenceExpression path = path(key("values"));
        final Modifier modifier = modifier(path);
        final JelMember member = JelMember.of(
            Alias.of(path), Json.value("value"));

        assertThrows(JelException.class, () -> modifier.modify(this.ctx, member));
    }

    private static Modifier modifier(final ReferenceExpression path) {
        final Modifier modifier = new SetModifier(
            new ParsedToken(TokenType.WORD, "set"));
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
