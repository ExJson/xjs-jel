package xjs.jel.modifier;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.jel.expression.ArrayGeneratorExpression;
import xjs.jel.expression.Expression;
import xjs.jel.expression.TupleExpression;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public final class ArrayGeneratorModifierTest {

    @Test
    public void modifyLogic_delegatesTo_arrayGeneratorExpression() {
        final Modifier modifier = new ArrayGeneratorModifier(
            new TupleExpression(
                new ContainerToken(TokenType.BRACES, List.of()),
                List.of()));
        final Expression modified = modifier.modify(Json.value(null));

        assertInstanceOf(ArrayGeneratorExpression.class, modified);
    }
}
