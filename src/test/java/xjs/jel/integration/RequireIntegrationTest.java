package xjs.jel.integration;

import org.junit.jupiter.api.Test;
import xjs.core.JsonValue;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RequireIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void require_loadsSingleFile() {
        this.inputSuccess("""
            >> require: require/a.xjs
            """);
        final File a = this.ctx.resolveFile("require/a.xjs");
        assertNotNull(a);
        assertTrue(this.ctx.getFileMap().containsKey(a.getAbsolutePath()));
    }

    @Test
    public void require_loadsMultipleFiles() {
        this.inputSuccess("""
            >> require: [
              require/a.xjs
              require/b.xjs
            ]
            """);
        final File a = this.ctx.resolveFile("require/a.xjs");
        assertNotNull(a);
        final File b = this.ctx.resolveFile("require/b.xjs");
        assertNotNull(b);
        final Map<String, JsonValue> map = this.ctx.getFileMap();
        assertTrue(map.containsKey(a.getAbsolutePath()));
        assertTrue(map.containsKey(b.getAbsolutePath()));
    }

    @Test
    public void require_loadsDirectory() {
        this.inputSuccess("""
            >> require: require
            """);
        final File a = this.ctx.resolveFile("require/a.xjs");
        assertNotNull(a);
        final File b = this.ctx.resolveFile("require/b.xjs");
        assertNotNull(b);
        final Map<String, JsonValue> map = this.ctx.getFileMap();
        assertTrue(map.containsKey(a.getAbsolutePath()));
        assertTrue(map.containsKey(b.getAbsolutePath()));
    }

    @Test
    public void require_whenFileIsNotFound_throwsException() {
        this.inputFailure("""
            >> require: not_found.xjs
            """);
        this.outputExactly("""
            JelException: File not found: not_found.xjs
            ---------------------------------------------------
                1 | >> require: not_found.xjs
                       ^^^^^^^""");
    }
}
