package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class DefaultValuesIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void defaultsModifier_copiesAbsentValues() {
        this.inputSuccess("""
            a >> private: { x: 1, y: 2, z: 3 }
            b >> $a: { w: 0 }
            """);
        this.outputTrimmed("""
            b: { w: 0, x: 1, y: 2, z: 3 }
            """);
    }

    @Test
    public void defaultsModifier_doesNotCopy_presentValues() {
        this.inputSuccess("""
            a >> private: { x: 0, y: 0, z: 0 }
            b >> $a: { x: 1, y: 2 }
            """);
        this.outputTrimmed("""
            b: { x: 1, y: 2, z: 0 }
            """);
    }

    @Test
    public void defaultsModifier_copiesValuesRecursively() {
        this.inputSuccess("""
            a >> private: { x: { a: 1, b: 2 }, y: { c: 3, d: 4 } }
            b >> $a: { x: { a: 0 }, y: { c: 0 } }
            """);
        this.outputTrimmed("""
            b: { x: { a: 0, b: 2 }, y: { c: 0, d: 4 } }
            """);
    }

    @Test
    public void defaultsModifier_whenSourceIsNotObject_throwsException() {
        this.inputFailure("""
            a: [ 1, 2, 3, ]
            b >> $a: {}
            """);
        this.outputExactly("""
            JelException: Cannot copy defaults from non-object value
            --------------------------------------------------------
                1 | b >> $a: {}
                         ^^
            --------------------------------------------------------
            Cannot copy from: [1,2,3]""");
    }

    @Test
    public void defaultsModifier_whenDestinationIsNotObject_throwsException() {
        this.inputFailure("""
            a: { x: 1 }
            b >> $a: []
            """);
        this.outputExactly("""
            JelException: Cannot copy defaults into non-object value
            --------------------------------------------------------
                1 | b >> $a: []
                         ^^  ^^
            --------------------------------------------------------
            Cannot copy: {"x":1}""");
    }

    @Test
    public void multipleDefaultModifiers_doCopyAllValues() {
        this.inputSuccess("""
            a >> private: { x: 1 }
            b >> private: { y: 2 }
            c >> $a $b: { z: 3 }
            """);
        this.outputTrimmed("""
            c: { z: 3, x: 1, y: 2 }
            """);
    }

    @Test
    public void defaultsModifier_isToleratedByTemplateExpression() {
        this.inputSuccess("""
            a >> private: { color: yellow }
            t >> (size) $a: { size: $size }
            b: $t(small)
            """);
        this.outputTrimmed("""
            b: { size: small, color: yellow }
            """);
    }
}
