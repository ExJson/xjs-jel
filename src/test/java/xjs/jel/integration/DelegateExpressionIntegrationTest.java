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
                0 | a >> @NotFound: null
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
                1 | c >> @T: null
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
}
