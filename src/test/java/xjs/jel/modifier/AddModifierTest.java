package xjs.jel.modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonArray;
import xjs.core.JsonReference;
import xjs.jel.Alias;
import xjs.jel.JelContext;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AddModifierTest {

    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
        this.ctx.pushParent(Json.object());
    }

    @Test
    public void modifiedExpression_addsValueIntoArray() throws JelException {
        final JsonArray values = Json.array(1, 2, 3);
        this.ctx.getScope().add("values", new JsonReference(values));

        final Modifier modifier = modifier(path(key("values")));
        final Expression modified = modifier.modify(Json.value(4));

        modified.apply(this.ctx);
        assertTrue(Json.array(1, 2, 3, 4).matches(values));
    }

    @Test
    public void modifiedExpression_whenOutputIsArray_addsAllValues() throws JelException {
        final JsonArray values = Json.array(1, 2, 3);
        this.ctx.getScope().add("values", new JsonReference(values));

        final Modifier modifier = modifier(path(key("values")));
        final Expression modified = modifier.modify(Json.array(4, 5, 6));

        modified.apply(this.ctx);
        assertTrue(Json.array(1, 2, 3, 4, 5, 6).matches(values));
    }

    @Test
    public void modifiedExpression_whenReferenceIsNotArray_convertsIntoArray() throws JelException {
        final JsonReference values = new JsonReference(Json.value(1));
        this.ctx.getScope().add("values", values);

        final Modifier modifier = modifier(path(key("values")));
        final Expression modified = modifier.modify(Json.value(2));

        modified.apply(this.ctx);
        assertTrue(Json.array(1, 2).matches(values.get()));
    }

    private static Modifier modifier(final ReferenceExpression path) {
        final Modifier modifier = new AddModifier(
            new ParsedToken(TokenType.WORD, "add"));
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
