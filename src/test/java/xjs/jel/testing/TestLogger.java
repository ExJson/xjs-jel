package xjs.jel.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class TestLogger extends Logger {
    private final List<LogRecord> output = new ArrayList<>();

    public TestLogger() {
        super("Test Logger", null);
    }

    @Override
    public void log(final LogRecord record) {
        this.output.add(record);
    }

    public List<LogRecord> getOutput() {
        return this.output;
    }
}
