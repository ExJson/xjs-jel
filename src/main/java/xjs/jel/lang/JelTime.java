package xjs.jel.lang;

import xjs.core.JsonNumber;
import xjs.jel.expression.Callable;
import xjs.jel.scope.CallableAccessor;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static xjs.jel.expression.LiteralExpression.of;

public class JelTime extends JsonNumber implements CallableAccessor {
    private static final List<String> KEYS =
        Arrays.asList("second", "minute", "hour", "english", "month", "day");

    private final OffsetDateTime time;

    public JelTime(final OffsetDateTime time) {
        super(time.toInstant().toEpochMilli());
        this.time = time;
    }

    @Override
    public Callable getCallable(final String key) {
        switch (key) {
            case "second": return (self, ctx, args) -> of(this.time.getSecond());
            case "minute": return (self, ctx, args) -> of(this.time.getMinute());
            case "hour": return (self, ctx, args) -> of(this.time.getHour());
            case "english": return (self, ctx, args) ->
                of(this.time.format(DateTimeFormatter.RFC_1123_DATE_TIME));
            case "month": return (self, ctx, args) ->
                of(this.time.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
            case "day": return (self, ctx, args) ->
                of(this.time.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        }
        return null;
    }

    @Override
    public List<String> callableKeys() {
        return KEYS;
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public String asString() {
        return this.time.toString();
    }

    @Override
    public String toString() {
        return this.time.toString();
    }

    @Override
    public JelTime copy(int options) {
        return withMetadata(new JelTime(this.time), this, options);
    }
}
