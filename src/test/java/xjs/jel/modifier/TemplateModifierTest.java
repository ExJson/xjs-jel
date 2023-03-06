package xjs.jel.modifier;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.jel.expression.Expression;
import xjs.jel.expression.TemplateExpression;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public final class TemplateModifierTest {

    @Test
    public void modifyLogic_delegatesTo_templateExpression() {
        final Modifier modifier = new TemplateModifier(
            new ContainerToken(TokenType.OPEN, List.of()), List.of());
        final Expression modified = modifier.modify(Json.value(null));

        assertInstanceOf(TemplateExpression.class, modified);
    }
}
