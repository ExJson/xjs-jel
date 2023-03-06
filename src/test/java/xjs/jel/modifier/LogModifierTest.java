package xjs.jel.modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ArrayExpression;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.sequence.JelType;
import xjs.jel.testing.TestLogger;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.io.File;
import java.util.List;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LogModifierTest {

    private TestLogger log;
    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.log = new TestLogger();
        this.ctx = new JelContext(new File(""), this.log);
    }

    @Test
    public void modifiedExpression_logsOutput() throws JelException {
        final Modifier modifier =
            new LogModifier(false, new ParsedToken(TokenType.WORD, "log"));
        final Expression modified = modifier.modify(Json.value(1234));

        modified.apply(this.ctx);
        assertEquals(1, this.log.getOutput().size());

        final LogRecord record = this.log.getOutput().get(0);
        assertTrue(record.getMessage().contains("1234"));
    }

    @Test
    public void modifiedExpression_whenRhsIsArrayExpression_logsEachValue() throws JelException {
        final Modifier modifier =
            new LogModifier(false, new ParsedToken(TokenType.WORD, "log"));
        final ArrayExpression exp = new ArrayExpression(
            new ContainerToken(TokenType.OPEN, List.of()),
            List.of(
                JelMember.builder(JelType.ELEMENT).expression(LiteralExpression.of(1)).build(),
                JelMember.builder(JelType.ELEMENT).expression(LiteralExpression.of(2)).build(),
                JelMember.builder(JelType.ELEMENT).expression(LiteralExpression.of(3)).build()));
        final Expression modified = modifier.modify(exp);

        modified.apply(this.ctx);
        assertEquals(3, this.log.getOutput().size());

        final LogRecord one = this.log.getOutput().get(0);
        final LogRecord two = this.log.getOutput().get(1);
        final LogRecord three = this.log.getOutput().get(2);
        assertTrue(one.getMessage().contains("1"));
        assertTrue(two.getMessage().contains("2"));
        assertTrue(three.getMessage().contains("3"));
    }

    @Test
    public void modifiedExpression_doesNotCatchException() {
        final Modifier modifier =
            new LogModifier(false, new ParsedToken(TokenType.WORD, "log"));
        final Expression modified = modifier.modify(ctx -> {
           throw new JelException("");
        });

        assertThrows(JelException.class, () -> modified.apply(this.ctx));
    }
}
