package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class JsonPathIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void simpleReference_yieldsValue() {
        this.inputSuccess("""
            a: 1234
            b: $a
            """);
        this.outputTrimmed("""
            a: 1234
            b: 1234
            """);
    }

    @Test
    public void simpleReference_toPrivateValue_yieldsValue() {
        this.inputSuccess("""
            a >> private: 1234
            b: $a
            """);
        this.outputTrimmed("""
            b: 1234
            """);
    }

    @Test
    public void formatting_ofGeneratedValue_comesFromReference() {
        this.inputSuccess("""
            a: 1234
            b:
              $a // comment
            """);
        this.outputTrimmed("""
            a: 1234
            b:
              1234 // comment
            """);
    }

    @Test
    public void formatting_ofGeneratedValue_preservesInternalFormatting_fromSource() {
        this.inputSuccess("""
            a: [
              1, 2
              3, 4
            ]
            b: $a
            """);
        this.outputTrimmed("""
            a: [
              1, 2
              3, 4
            ]
            b: [
              1, 2
              3, 4
            ]
            """);
    }

    @Test
    public void reference_afterAdditionalFramesHaveBeenPushed_andDropped_yieldsOriginalValue() {
        this.inputSuccess("""
            a: 1234
            b: [
              { a: 5678, c: $a }
              { a: 9010 }
              { a: 1112 }
            ]
            c: $a
            """);
        this.outputTrimmed("""
            a: 1234
            b: [
              { a: 5678, c: 5678 }
              { a: 9010 }
              { a: 1112 }
            ]
            c: 1234
            """);
    }

    @Test
    public void reference_resolvesSingleValue_fromComplexPath() {
        this.inputSuccess("""
            a >> private: {
              b: [ [ null, null, { c >> (): { d: value } } ] ]
            }
            b: $a.b[0][2].c().d
            """);
        this.outputTrimmed("""
            b: value
            """);
    }

    @Test
    public void reference_resolvesMultipleValues_fromComplexPath_intoArray() {
        this.inputSuccess("""
            a >> private: [
              null
              { b: { c: [ null, 1, 2 ] } }
              { b: { c: [ null, 3, 4 ] } }
              { b: { c: [ null, 5, 6 ] } }
              null
            ]
            b: $a[1:3].b.c[1:2]
            """);
        this.outputTrimmed("""
            b: [ 1, 2, 3, 4, 5, 6 ]
            """);
    }

    @Test
    public void reference_updatesMultipleValues_fromComplexPath() {
        this.inputSuccess("""
            a: [
              { b: { c: [ null, 1, 2 ] } }
              { b: { c: [ null, 3, 4 ] } }
              { b: { c: [ null, 5, 6 ] } }
              { b: { c: [ null, 7, 8 ] } }
              { b: { c: [ null, 9, 0 ] } }
            ]
            $a[1:3].b.c[1:2] >> set: x
            """);
        this.outputTrimmed("""
            a: [
              { b: { c: [ null, 1, 2 ] } }
              { b: { c: [ null, x, x ] } }
              { b: { c: [ null, x, x ] } }
              { b: { c: [ null, x, x ] } }
              { b: { c: [ null, 9, 0 ] } }
            ]
            """);
    }

    @Test
    public void reference_whenPathIsNotResolved_returnsNull() {
        this.inputSuccess("""
            a >> private: {
              b: value
            }
            c: $x.y
            """);
        this.outputTrimmed("""
            c: null
            """);
    }

    @Test
    public void reference_withStrictPathing_whenPathIsNotResolved_throwsException() {
        this.ctx.setStrictPathing(true);
        this.inputFailure("""
            a >> private: {
              b: value
            }
            c: $x.y
            """);
        this.outputExactly("""
            JelException: Path does not resolve to any variable
            -----------------------------------------------------
                4 | c: $x.y
                       ^^^^
            -----------------------------------------------------
            Application is configured to disallow lenient pathing""");
    }

    @Test
    public void reference_withStrictPathing_whenPathResolvesToNull_returnsNull() {
        this.ctx.setStrictPathing(true);
        this.inputSuccess("""
            a >> private: {
              b: null
            }
            c: $a.b
            """);
        this.outputTrimmed("""
            c: null
            """);
    }

}
