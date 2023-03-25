package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class AddModifierIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void add_insertsValue_intoAnArray() {
        this.inputSuccess("""
            a: [ 1 ]
            a >> add: 2
            """);
        this.outputTrimmed("""
            a: [1
              2]
            """); // formatting needs work
    }

    @Test
    public void add_whenDestinationIsNotAnArray_convertsIntoArray() {
        this.inputSuccess("""
            a: 1
            a >> add: 2
            """);
        this.outputTrimmed("""
            a: [1
              2
            ]
            """); // formatting needs work
    }

    @Test
    public void add_whenRhsIsArray_addsEachValue() {
        this.inputSuccess("""
            a: [ 1 ]
            a >> add: [ 2, 3 ]
            """);
        this.outputTrimmed("""
            a: [ 1, 2, 3 ]
            """);
    }

    @Test
    public void add_whenDestinationIsNotSpecified_throwsException() {
        this.inputFailure("""
            >> add: 1
            """);
        this.outputExactly("""
            JelException: Missing alias
            ---------------------------------------------------
                1 | >> add: 1
                       ^^^
            ---------------------------------------------------
            Hint: this modifier requires an alias""");
    }
}
