package xjs.jel.integration;

import org.junit.jupiter.api.Test;
import xjs.core.JsonValue;
import xjs.jel.JelFlags;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JelFlagsIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void privateValue_isInvisibleFromOutput() {
        this.inputSuccess("""
            a >> private: value
            b: value
            """);
        this.outputTrimmed("""
            b: value
            """);
    }

    @Test
    public void privateValue_isInvisibleOutOfScope() {
        this.inputSuccess("""
            a: {
              b >> private: value
            }
            c: $a.b
            """);
        this.outputTrimmed("""
            a: {
            }
            c: null
            """);
    }

    @Test // this is _supposed_ to be an error about illegal access, impl tbd
    public void readingPrivateValue_withStrictPathing_throwsException() {
        this.ctx.setStrictPathing(true);
        this.inputFailure("""
            a: {
              b >> private: value
            }
            c: $a.b
            """);
        this.outputExactly("""
            JelException: Path does not resolve to any variable
            -----------------------------------------------------
                4 | c: $a.b
                       ^^^^
            -----------------------------------------------------
            Application is configured to disallow lenient pathing""");
    }

    @Test // this is _supposed_ to be invisible from output. impl tbd
    public void varValue_hasVarFlag() {
        this.inputSuccess("""
            a >> var: null
            """);
        assertTrue(this.valueOut.isObject());
        final JsonValue a = this.valueOut.asObject().get("a");
        assertNotNull(a);
        assertTrue(a.hasFlag(JelFlags.VAR));
    }

    @Test // this is _supposed_ to be invisible from output. impl tbd
    public void noinlineValue_hasNoinlineFlag() {
        this.inputSuccess("""
            a >> noinline: null
            """);
        assertTrue(this.valueOut.isObject());
        final JsonValue a = this.valueOut.asObject().get("a");
        assertNotNull(a);
        assertTrue(a.hasFlag(JelFlags.NOINLINE));
    }

    @Test
    public void template() {
        this.inputSuccess("""
            """);
        this.outputTrimmed("""
            """);
    }
}
