package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class GeneratorExpressionIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void generator_returnsValuesFromInput() {
        this.inputSuccess("""
            a >> [ 1, 2, 3 ]: $v
            """);
        this.outputTrimmed("""
            a: [
              1
              2
              3
            ]
            """);
    }

    @Test
    public void generator_copiesValuesFromExpansion() {
        this.inputSuccess("""
            a >> private: [ x, y, z ]
            b >> [ a, b, c, $a.. ]: $v
            """);
        this.outputTrimmed("""
            b: [
              a
              b
              c
              x
              y
              z
            ]
            """);
    }

    @Test
    public void generator_includesIndexInScope() {
        this.inputSuccess("""
            a >> [ a, b, c ]: $i
            """);
        this.outputTrimmed("""
            a: [
              0
              1
              2
            ]
            """);
    }

    @Test
    public void generator_isSupportedByTemplateExpression() {
        this.inputSuccess("""
            a >> (values) [ $values.., 3, 4 ]: $v
            b: $a([1, 2])
            """);
        this.outputTrimmed("""
            b: [
              1
              2
              3
              4
            ]
            """);
    }

    @Test
    public void generator_withConditional_filtersExpression() {
        this.inputSuccess("""
            numbers >> private: [ 1, 2, 3, 4, 5, 6 ]
            evens >> [ $numbers.. ] if ($v % 2 == 0): $v
            """);
        this.outputTrimmed("""
            evens: [
              2
              4
              6
            ]
            """);
    }
}
