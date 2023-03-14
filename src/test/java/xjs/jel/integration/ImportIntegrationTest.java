package xjs.jel.integration;

import org.junit.jupiter.api.Test;
import xjs.jel.exception.JelException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ImportIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void simpleImport_copiesValues_intoField() {
        this.inputSuccess("""
            demo >> import: hello_world.xjs
            """);
        this.outputTrimmed("""
            demo: {
              hello: world!
              values: { a: 1, b: 2, c: 3 }
            }
            """);
    }

    @Test
    public void simpleImport_withNoAlias_copiesValues_intoParent() {
        this.inputSuccess("""
            >> import: hello_world.xjs
            """);
        this.outputTrimmed("""
            hello: world!
            values: { a: 1, b: 2, c: 3 }
            """);
    }

    @Test
    public void import_doesImportTemplates() {
        this.inputSuccess("""
            >> import: templates.xjs
            out: $get_hello()
            """);
        this.outputTrimmed("""
            out: 'hello, world!'
            """);
    }

    @Test
    public void import_doesTolerateDestructuring() {
        this.inputSuccess("""
            { values } >> import from: hello_world.xjs
            """);
        this.outputTrimmed("""
            values: { a: 1, b: 2, c: 3 }
            """);
    }

    @Test
    public void importFrom_doesImportTemplates() {
        this.inputSuccess("""
            { get_hello } >> import from: templates.xjs
            out: $get_hello()
            """);
        this.outputTrimmed("""
            out: 'hello, world!'
            """);
    }

    @Test
    public void import_canReadJsonFiles() {
        this.inputSuccess("""
            data >> import: data.json
            """);
        this.outputTrimmed("""
            data: {
              key: value
            }
            """);
    }

    @Test
    public void import_canReadOtherFormats_withXjsCompat() {
        this.inputSuccess("""
            text >> import: text.txt
            """);
        this.outputTrimmed("""
            text:\s
              '''
              plain text
              in this file
              '''
            """);
    }

    @Test
    public void import_whenTemplateThrowsException_correctlyHandlesScope() {
        this.inputFailure("""
            >> import: scoped_errors.xjs
            >>: $scoped_throw()
            """);
        this.outputExactly("""
            JelException: demo error
            ---------------------------------------------------
                2 | >>: $scoped_throw()
                         ^^^^^^^^^^^^^^
            ---------------------------------------------------
            In file: templates.xjs
            ---------------------------------------------------
                3 | throw >> () raise: 'demo error'
                                ^^^^^
            ---------------------------------------------------
            In file: scoped_errors.xjs
            ---------------------------------------------------
                2 | scoped_throw >> () : $throw()
                                          ^^^^^^^""");
    }

    @Test
    public void import_withSyntaxErrors_throwsException() {
        this.inputFailure("""
            >> import: syntax_errors.xjs
            """);
        this.outputTrimmed("""
            JelException: Dependency not loaded: syntax_errors.xjs
            """);
    }

    @Test
    public void import_withCyclicalImports_isHandledByContext() {
        final JelException e = assertThrows(JelException.class,
            () -> this.ctx.getImport("cyclical_a.xjs"));

        assertEquals("Dependency not loaded: cyclical_a.xjs", e.getMessage());

        final JelException cyclical = this.ctx.getError("cyclical_b.xjs");
        assertNotNull(cyclical);
        assertEquals("Illegal cyclical reference", cyclical.getMessage());
        assertEquals("Hint: this file is also being loaded by cyclical_a.xjs", cyclical.getDetails());
    }
}
