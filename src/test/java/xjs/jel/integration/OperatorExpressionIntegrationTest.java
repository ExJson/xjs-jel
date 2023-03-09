package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class OperatorExpressionIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void binomialArithmetic_isInlined() {
        this.inputSuccess("""
            a: 1 + 2
            """);
        this.outputTrimmed("""
            a: 3
            """);
    }

    @Test
    public void arithmetic_withAdditionalOperators_isInlined() {
        this.inputSuccess("""
            a: 1 + 2 * 3
            """);
        this.outputTrimmed("""
            a: 7
            """);
    }

    @Test
    public void binomialBoolean_isInlined() {
        this.inputSuccess("""
            a: 1 > 0 && 3 < 4
            """);
        this.outputTrimmed("""
            a: true
            """);
    }

    @Test
    public void expression_startingWithMultipleWords_isNotInlined() {
        this.inputSuccess("""
            a: 1 2 + 3
            """);
        this.outputTrimmed("""
            a: 1 2 + 3
            """);
    }

    @Test
    public void noinlineExpression_isNotInlined() {
        this.inputSuccess("""
            a >> noinline: 1 + 2 + 3
            """);
        this.outputTrimmed("""
            a: 1 + 2 + 3
            """);
    }

    @Test
    public void divideByZero_isCaughtByEvaluator() {
        this.inputFailure("""
            a: 1 % 0
            """);
        this.outputExactly("""
            JelException: Expression divides by zero
            ---------------------------------------------------
                0 | a: 1 % 0
                           ^""");
    }
}
