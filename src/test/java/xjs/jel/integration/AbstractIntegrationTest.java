package xjs.jel.integration;

import org.junit.jupiter.api.BeforeEach;
import xjs.core.JsonFormat;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.Sequence;
import xjs.jel.serialization.sequence.Sequencer;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger("Error Formatter");

    protected JelContext ctx;
    protected String fullText;
    protected JsonValue valueOut;
    protected String errorOut;
    protected boolean expectingFailure;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File("integration"));
        this.fullText = null;
        this.valueOut = null;
        this.errorOut = null;
        this.expectingFailure = false;
    }

    protected JsonValue parse(final String text) throws JelException {
        return this.ctx.eval(this.sequence(text));
    }

    protected Sequence<?> sequence(final String text) throws JelException {
        try {
            return Sequencer.JEL.parse(text);
        } catch (final JelException e) {
            if (!this.expectingFailure) {
                LOGGER.log(Level.SEVERE, "\n\n" + e.format(text));
            }
            throw e;
        }
    }

    protected void inputSuccess(final String fullText) {
        try {
            this.fullText = fullText;
            this.valueOut = this.parse(fullText);
        } catch (final JelException e) {
            LOGGER.log(Level.SEVERE, "\n\n" + e.format(fullText));
            fail("Expected happy path", e);
        }
    }

    protected void inputFailure(final String fullText) {
        final JsonValue value;
        try {
            this.fullText = fullText;
            this.expectingFailure = true;
            value = this.parse(fullText);
        } catch (final JelException e) {
            this.errorOut = e.format(fullText);
            return;
        }
        fail("Expected JelException, but no exception was thrown\n" + value);
    }

    protected void outputExactly(final String expected) {
        if (this.valueOut == null) {
            assertNotNull(this.errorOut, "No error in output");
            assertEquals(normalize(expected), normalize(this.errorOut));
        } else {
            assertEquals(
                normalize(expected),
                normalize(this.valueOut.toString(JsonFormat.XJS_FORMATTED)));
        }
    }

    protected void outputTrimmed(final String expected) {
        if (this.valueOut == null) {
            assertNotNull(this.errorOut, "No error in output");
            assertEquals(
                normalize(expected).trim(),
                normalize(this.errorOut).trim());
        } else {
            assertEquals(
                normalize(expected).trim(),
                normalize(this.valueOut.toString(JsonFormat.XJS_FORMATTED)).trim());
        }
    }

    private static String normalize(final String s) {
        return s.replaceAll("\r?\n", "\n");
    }
}
