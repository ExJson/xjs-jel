package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class ConditionalExpressionIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void conditionalModifier_whenConditionIsMet_outputsMember() {
        this.inputSuccess("""
            a >> private: 100
            b >> if ($a > 50): value
            """);
        this.outputTrimmed("""
            b: value
            """);
    }

    @Test
    public void conditionalModifier_whenConditionIsNotMet_doesNotOutputMember() {
        this.inputSuccess("""
            a >> private: 100
            b >> if ($a < 50): value
            """);
        this.outputExactly("{\n}");
    }

    @Test
    public void conditionalExpression_outputsFirstMatchingCondition() {
        this.inputSuccess("""
            a >> private: 100
            b >> if: {
              $a < 50: null
              $a < 100: null
              $a < 150: value
              _: null
            }
            """);
        this.outputTrimmed("""
            b: value
            """);
    }

    @Test // pending design changes
    public void conditionalExpression_whenNoConditionIsMet_returnsNull() {
        this.inputSuccess("""
            a >> private: 100
            b >> if: {
              $a < 50: value
              $a < 100: value
            }
            """);
        this.outputTrimmed("""
            b: null
            """);
    }

    @Test
    public void conditionalExpression_withStrictPathing_whenNoConditionIsMet_throwsException() {
        this.ctx.setStrictPathing(true);
        this.inputFailure("""
            a >> private: 100
            b >> if: {
              $a < 50: value
              $a < 100: value
            }
            """);
        this.outputExactly("""
            JelException: No condition matched. Missing default case?
            ---------------------------------------------------------
                1 | b >> if: {
                             ^
                2 |   $a < 50: value
                    ^^^^^^^^^^^^^^^^
                3 |   $a < 100: value
                    ^^^^^^^^^^^^^^^^^
                4 | }
                    ^
            ---------------------------------------------------------
            Hint: this application disallows lenient pathing""");
    }

    @Test
    public void conditionalExpression_withDefaultCase_whenNoConditionIsMet_outputsDefaultCase() {
        this.inputSuccess("""
            a >> private: 100
            b >> if: {
              $a < 50: null
              $a < 100: null
              _: value
            }
            """);
        this.outputTrimmed("""
            b: value
            """);
    }

    @Test
    public void conditionalExpression_doesEvaluateNestedExpressions() {
        this.inputSuccess("""
            a >> private: 100
            b >> private: -50
            c >> if: {
              $a < 50: null
              $a < 150 >> if: {
                $b > 0: null
                $b > -25: null
                $b > -100: value
              }
              _: null
            }
            """);
        this.outputTrimmed("""
            c: value
            """);
    }
}
