package xjs.jel;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonValue;
import xjs.jel.exception.JelException;
import xjs.jel.lang.JelFunctions;

public final class JelFunctionsTest {

    @Test
    @SuppressWarnings("ConstantConditions")
    public void min_withTwoParameters_getsLowestValue() throws JelException {
        final JsonValue value =
            JelFunctions.lookup("min")
                .call(null, null, Json.value(1234), Json.value(5678))
                .apply(null);
    }
}
