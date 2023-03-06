package xjs.jel.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.path.KeyComponent;
import xjs.jel.path.PathComponent;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ArrayGeneratorExpressionTest {

    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
        this.ctx.pushParent(Json.object());
    }

    @Test
    public void simpleGenerator_returnsValues() throws JelException {
        final Expression exp = exp(values(1, 2, 3), path(key("v")));
        assertEquals(Json.array(1, 2, 3), exp.apply(this.ctx));
    }

    @Test
    public void generator_evaluatesExpressions() throws JelException {
        final Expression exp = exp(
            values(1, 2, 3),
            math(num(2), op(Operator.MULTIPLY), path(key("v"))));

        assertEquals(Json.array(2, 4, 6), exp.apply(this.ctx));
    }

    @Test
    public void simpleGenerator_returnsIndices() throws JelException {
        final Expression exp = exp(values("a", "b", "c"), path(key("i")));
        assertEquals(Json.array(0, 1, 2), exp.apply(this.ctx));
    }

    private static ArrayGeneratorExpression exp(
            final TupleExpression input, final Expression out) {
        return new ArrayGeneratorExpression(input, out);
    }

    private static TupleExpression values(final Object... values) {
        final List<Span<?>> out = new ArrayList<>();
        for (final Object value : values) {
            out.add(LiteralExpression.of(Json.any(value)));
        }
        return new TupleExpression(
            new ContainerToken(TokenType.BRACKETS, List.of()),
            out);
    }

    private static ArithmeticExpression math(final Sequence<?>... subs) {
        return new ArithmeticExpression(List.of(subs), true);
    }

    private static LiteralExpression num(final double number) {
        return LiteralExpression.of(number);
    }

    private static OperatorSequence op(final Operator op) {
        return new OperatorSequence(op, 0);
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
