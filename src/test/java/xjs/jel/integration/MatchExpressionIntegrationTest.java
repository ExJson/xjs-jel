package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class MatchExpressionIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void matchExpression_selectsCaseByEquality() {
        this.inputSuccess("""
            a >> match $(456): {
              123: null
              456: value
              789: null
            }
            """);
        this.outputTrimmed("""
            a: value
            """);
    }

    @Test
    public void matchExpression_whenNoMatchIsFound_returnsNull() {
        this.inputSuccess("""
            a >> match $(101): {
              123: null
              456: value
              789: null
            }
            """);
        this.outputTrimmed("""
            a: null
            """);
    }

    @Test
    public void matchExpression_withStrictPathing_whenNoMatchIsFound_throwsException() {
        this.ctx.setStrictPathing(true);
        this.inputFailure("""
            a >> match $(101): {
              123: null
              456: value
              789: null
            }
            """);
        this.outputExactly("""
            JelException: No condition matched. Missing default case?
            ---------------------------------------------------------
                1 | a >> match $(101): {
                                       ^
                2 |   123: null
                    ^^^^^^^^^^^
                3 |   456: value
                    ^^^^^^^^^^^^
                4 |   789: null
                    ^^^^^^^^^^^
                5 | }
                    ^
            ---------------------------------------------------------
            Hint: this application disallows lenient pathing""");
    }

    @Test
    public void matchExpression_withDefaultCase_whenNoMatchIsFound_returnsDefaultCase() {
        this.inputSuccess("""
            a >> match $(101): {
              123: null
              _: value
            }
            """);
        this.outputTrimmed("""
            a: value
            """);
    }

    @Test
    public void matchExpression_doesEvaluateNestedExpressions() {
        this.inputSuccess("""
            a >> private: 1
            b >> private: 2
            c >> match $a: {
              0: null
              1 >> match $b: {
                0: null
                1: null
                2: value
                _: null
              }
              _: null
            }
            """);
        this.outputTrimmed("""
            c: value
            """);
    }

    @Test
    public void matchArray_isLiteralArrayForNow() {
        this.inputSuccess("""
            a >> private: 1
            b >> private: 2
            out >> match $([$a, $b]): {
              [ 3, 4 ]: failure
              [ 5, 6 ]: sadness
              [ 7, 8 ]: misery
              [ 1, 2 ]: happiness
              _: doom
            }
            """);
        this.outputTrimmed("""
            out: happiness
            """);
    }
}
