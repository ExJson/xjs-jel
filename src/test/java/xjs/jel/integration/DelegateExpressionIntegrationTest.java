package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class DelegateExpressionIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void delegate_passesRhsIntoCallable() {
        this.inputSuccess("""
            a >> @array: 1234
            """);
        this.outputTrimmed("""
            a: [
              1234
            ]
            """);
    }

    @Test
    public void delegate_whenCallableIsNotFound_throwsException() {
        this.inputFailure("""
            a >> @NotFound: null
            """);
        this.outputExactly("""
            JelException: Callable not in scope: NotFound
            ---------------------------------------------------
                1 | a >> @NotFound: null
                          ^^^^^^^^
            ---------------------------------------------------
            Expected a template or function named 'NotFound'""");
    }

    @Test
    public void delegate_whenCallableRequiresDifferentArguments_throwsException() {
        this.inputFailure("""
            T >> (a, b): null
            c >> @T: null
            """);
        this.outputExactly("""
            IllegalJelArgsException: expected 2 arguments
            ---------------------------------------------------
                2 | c >> @T: null
                          ^""");
    }

    @Test
    public void delegate_isToleratedByTemplate() {
        this.inputSuccess("""
            into_array >> @array (v): $v
            a: $into_array(1234)
            """);
        this.outputTrimmed("""
            a: [
              1234
            ]
            """);
    }

    @Test
    public void delegate_mayBeReturnedBy_anyReference() {
        this.inputSuccess("""
            t >> (a) (b): {
              a: $a
              b: $b
            }
            r >> @t(1): 2
            """);
        this.outputTrimmed("""
            r: {
              a: 1
              b: 2
            }
            """);
    }

    @Test
    public void delegate_whenReferenceDoesNotReturnCallable_throwsSpecificException() {
        this.inputFailure("""
            a: [ 0 ]
            r >> @a[0]: xyz
            """);
        this.outputExactly("""
            JelException: Callable not in scope
            ---------------------------------------------------
                2 | r >> @a[0]: xyz
                          ^^^^
            ---------------------------------------------------
            Callable not returned by JSON path""");
    }

    @Test
    public void nestedCall_isResolvedByDelegate() {
        this.inputSuccess("""                            
            typed >> (type) (cfg) if: {
              $cfg.isObject() >> $cfg: {
                type: $type
              }
              _: {
                type: $type
                value: $cfg
              }
            }
                        
            vanilla >> (type) (cfg): {
              vanilla >> @typed($type): $cfg
            }
                        
            x: $vanilla(absolute)(1234)
            """);
        this.outputTrimmed("""
            x: {
              vanilla: {
                type: absolute
                value: 1234
              }
            }
            """);
    }
}
