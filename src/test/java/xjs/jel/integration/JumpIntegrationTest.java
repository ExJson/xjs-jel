package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class JumpIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void yield_yieldsValue_toParentScope() {
        this.inputSuccess("""
            return: {
              a: 123
              b: 456
              >> yield: $a + $b
            }
            """);
        this.outputTrimmed("""
            return: 579
            """);
    }

    @Test
    public void yield_inArrayGenerator_doesNotChangeBehavior() {
        this.inputSuccess("""
            array >> [ 1, 2, 3 ]: {
              double: $v * 2
              >> yield: $double + 1
            }
            """);
        this.outputTrimmed("""
            array: [
              3
              5
              7
            ]
            """);
    }

    @Test
    public void return_yieldsValue_fromTemplateExpression() {
        this.inputSuccess("""
            t >> (): {
              >> [1, 2, 3]: {
                >> if ($v % 2 == 0) return: $v
              }
              >> raise: unreachable
            }
            r: $t()
            """);
        this.outputTrimmed("""
            r: 2
            """);
    }

    @Test
    public void return_takesPrecedentOverYield() {
        // can be written with less code. just a test
        this.inputSuccess("""
            x >> [ 1, 3, 5, 6 ]: {
              >> if ($v % 2 != 0) yield: $v
              >> if ($v % 2 == 0) return: $v
            }
            """);
        this.outputExactly("6");
    }
}
