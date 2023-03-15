package xjs.jel.integration;

import org.junit.jupiter.api.Test;
import xjs.core.JsonValue;
import xjs.jel.JelFlags;
import xjs.jel.lang.JelObject;

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

    @Test
    public void var_isInvisibleFromOutput() {
        this.inputSuccess("""
            a >> var: value
            b: value
            """);
        this.outputTrimmed("""
            b: value
            """);
    }

    @Test
    public void var_isNotInvisibleOutOfScope() {
        this.inputSuccess("""
            a: {
              b >> var: value
            }
            c: $a.b
            """);
        this.outputTrimmed("""
            a: {
            }
            c: value
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

    @Test
    public void varValue_hasVarFlag() {
        this.inputSuccess("""
            a >> var: null
            """);
        assertTrue(this.valueOut.isObject());
        final JsonValue a = ((JelObject) this.valueOut).getDeclared("a");
        assertNotNull(a);
        assertTrue(a.hasFlag(JelFlags.VAR));
    }

    @Test
    public void noinlineValue_hasNoinlineFlag() {
        this.inputSuccess("""
            a >> noinline: null
            """);
        assertTrue(this.valueOut.isObject());
        final JsonValue a = this.valueOut.asObject().get("a");
        assertNotNull(a);
        assertTrue(a.hasFlag(JelFlags.NOINLINE));
    }
}
