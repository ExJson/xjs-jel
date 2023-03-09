package xjs.jel.integration;

import org.junit.jupiter.api.Test;

public final class DestructurePatternIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void objectDestructure_copiesKeysFromObject() {
        this.inputSuccess("""
            { a, b } >> from: { a: 1, b: 2, c: 3 }
            """);
        this.outputTrimmed("""
            a: 1, b: 2
            """);
    }

    @Test
    public void objectDestructure_canRenameKeys() {
        this.inputSuccess("""
            { x: a, y: b } >> from: { a: 1, b: 2 }
            """);
        this.outputTrimmed("""
            x: 1, y: 2
            """);
    }

    @Test
    public void objectDestructure_copiesTemplate() {
        this.inputSuccess("""
            { t } >> from: { t >> (): 123 }
            a: $t()
            """);
        this.outputTrimmed("""
            a: 123
            """);
    }

    @Test
    public void objectDestructure_whenTemplateIsNotListed_doesNotCopyTemplate() {
        this.inputSuccess("""
            {} >> from: { t >> (): 123 }
            a: $t()
            """);
        this.outputTrimmed("""
            a: null
            """);
    }

    @Test
    public void objectDestructure_whenSourceIsNotObject_throwsException() {
        this.inputFailure("""
            { a } >> from: [ 1 ]
            """);
        this.outputExactly("""
            JelException: Cannot destructure array as object
            ---------------------------------------------------
                0 | { a } >> from: [ 1 ]
                    ^^^^^
            ---------------------------------------------------
            Cannot destructure: [1]""");
    }

    @Test
    public void arrayDestructure_copiesValuesByIndex() {
        this.inputSuccess("""
            [ a, b, c ] >> from: [ 1, 2, 3, 4 ]
            """);
        this.outputTrimmed("""
            a: 1, b: 2, c: 3
            """);
    }

    @Test
    public void arrayDestructure_withEllipsisOperator_readsSomeValuesFromEnd() {
        this.inputSuccess("""
            [ a, b .. c, d ] >> from: [ 1, 2, 3, 4, 5, 6 ]
            """);
        this.outputTrimmed("""
            a: 1, b: 2, c: 5, d: 6
            """);
    }

    @Test
    public void arrayDestructure_toleratesReadingFromEndOnly() {
        this.inputSuccess("""
            [ .. a, b ] >> from: [ 1, 2, 3, 4 ]
            """);
        this.outputTrimmed("""
            a: 3, b: 4
            """);
    }

    @Test
    public void arrayDestructure_toleratesRecursivePatterns() {
        this.inputSuccess("""
            [ [ a ], [ { b: x } ] ] >> from: [ [ 1, 2 ], [ { x: 3 } ] ]
            """);
        this.outputTrimmed("""
            a: 1, b: 3
            """);
    }
}
