package xjs.jel.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.jel.testing.TestLogger;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LoggingIntegrationTest extends AbstractIntegrationTest {

    private TestLogger log;

    @BeforeEach
    public void setup() {
        super.setup();
        this.log = new TestLogger();
        this.ctx.setLog(this.log);
    }

    @Test
    public void logExpression_logsOutput() {
        this.inputSuccess("""
            >> log: 1234
            """);

        final List<LogRecord> output = this.log.getOutput();
        assertEquals(1, output.size());
        assertEquals(Level.INFO, output.get(0).getLevel());
        assertTrue(output.get(0).getMessage().contains("1234"));
    }

    @Test
    public void logError_logsOutput_asError() {
        this.inputSuccess("""
            >> log error: 1234
            """);

        final List<LogRecord> output = this.log.getOutput();
        assertEquals(1, output.size());
        assertEquals(Level.SEVERE, output.get(0).getLevel());
        assertTrue(output.get(0).getMessage().contains("1234"));
    }

    @Test
    public void logExpression_withMultipleValues_logsEachValueSeparately() {
        this.inputSuccess("""
            >> log: [ 1, 2, 3, 4 ]
            """);

        final List<LogRecord> output = this.log.getOutput();
        assertEquals(4, output.size());
        assertTrue(output.get(0).getMessage().contains("1"));
        assertTrue(output.get(1).getMessage().contains("2"));
        assertTrue(output.get(2).getMessage().contains("3"));
        assertTrue(output.get(3).getMessage().contains("4"));
    }

    @Test
    public void logExpression_withArray_logsArray_asASingleValue() {
        this.inputSuccess("""
            a: [ 1, 2, 3, 4 ]
            >> log: $a
            """);

        final List<LogRecord> output = this.log.getOutput();
        assertEquals(1, output.size());
        assertTrue(output.get(0).getMessage().contains("[1,2,3,4]"));
    }

    @Test
    public void logExpression_withLiteralObject_logsEachMemberSeparately() {
        this.inputSuccess("""
            >> log: { a: 1, b: 2 }
            """);

        final List<LogRecord> output = this.log.getOutput();
        assertEquals(2, output.size());
        assertTrue(output.get(0).getMessage().contains("a: 1"));
        assertTrue(output.get(1).getMessage().contains("b: 2"));
    }

    @Test
    public void logExpression_withObject_logsAsASingleValue() {
        this.inputSuccess("""
            o: { a: 1, b: 2 }
            >> log: $o
            """);

        final List<LogRecord> output = this.log.getOutput();
        assertEquals(1, output.size());
        assertTrue(output.get(0).getMessage().contains("{\"a\":1,\"b\":2}"));
    }
}
