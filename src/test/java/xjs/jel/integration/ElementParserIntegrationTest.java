package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class ElementParserIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void singleElement_doesNotTolerate_danglingTokens() {
        this.inputFailure("""
            string here
            "this ain't legal"
            """);
        this.outputExactly("""
            JelException: illegal dangling tokens after expression
            -----------------------------------------------------------------------------------
                2 | "this ain't legal"
                    ^^^^^^^^^^^^^^^^^^
            -----------------------------------------------------------------------------------
            Hint: objects and arrays may not have trailing tokens
            Hint: some modifiers expect single-token values""");
    }

    @Test
    public void multipleKeys_whereAnyValue_hasMultipleLinesBeforeItsValue_toleratesWhitespace() {
        this.inputSuccess("""
            a:
              1
            b
              :
              2
            c:
            
            
              3
            """);
        this.outputTrimmed("""
            a:
              1
            b:
            
              2
            c:
            
            
              3
            """);
    }
}
