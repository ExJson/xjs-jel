package xjs.jel.serialization.util;

import xjs.core.StringType;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.Token;
import xjs.serialization.util.PositionTrackingReader;

public class Rereader extends PositionTrackingReader {
    private final CharSequence source;
    private final StringToken token;
    private final String text;
    private final boolean multiline;
    public int absIndex;
    public int absLine;
    public int absColumn;

    public Rereader(final CharSequence source, final StringToken token) {
        this.source = source;
        this.token = token;
        this.text = token.parsed();
        this.multiline = isMultiline(token.stringType());

        this.initAbsolute();
        this.read();
    }

    private static boolean isMultiline(final StringType type) {
        return type == StringType.MULTI || type == StringType.IMPLICIT;
    }

    private void initAbsolute() {
        final Token t = this.token;
        switch (t.stringType()) {
            case SINGLE:
            case DOUBLE:
                this.absIndex = t.start();
                this.absColumn = t.offset();
                this.absLine = t.line();
                break;
            case MULTI:
                this.initMulti();
                break;
            default:
                this.absIndex = t.start() - 1;
                this.absColumn = t.offset() - 1;
                this.absLine = t.line();
        }
    }

    private void initMulti() {
        final CharSequence s = this.source;
        final Token t = this.token;
        boolean nl = false;
        int i = t.start() + 3;
        char c;
        // skip to nl
        for (; i < s.length(); i++) {
            c = s.charAt(i);
            if (!Character.isWhitespace(c)) {
                break;
            } else if (c == '\n') {
                nl = true;
                break;
            }
        }
        // skip to offset
        if (nl) {
            final int lineIndex = ++i;
            final int expected = i + t.offset();
            for (; i < expected; i++) {
                c = s.charAt(i);
                if (!Character.isWhitespace(c) || c == '\n') {
                    break;
                }
            }
            this.absLine = t.line() + 1;
            this.absColumn = i - lineIndex - 1;
        } else {
            this.absLine = t.line();
            this.absColumn = t.offset() + 3 - 1;
        }
        this.absIndex = i - 1;
    }

    @Override
    public CharSequence getFullText() {
        return this.source;
    }

    @Override
    protected void appendToCapture() {
        this.capture.append(this.text, this.captureStart, this.index);
    }

    @Override
    protected String slice() {
        return this.text.substring(this.captureStart, this.index);
    }

    @Override
    public void read() {
        if (this.current == -1) {
            return;
        }
        if (this.index == this.text.length() - 1) {
            this.index = this.text.length();
            this.updateAbs();
            this.current = -1;
            return;
        }
        if (this.current == '\n') {
            this.line++;
            this.linesSkipped++;
            this.column = -1;
        }
        this.updateAbs();
        this.current = this.text.charAt(++this.index);
        this.column++;
    }

    private void updateAbs() {
        if (this.multiline) {
            if (this.current == '\n') {
                this.absLine++;
                this.absColumn = this.findAbsColumn() - 1;
                this.absIndex += this.absColumn + 1;
            }
        } else if (this.requiresEscape()) {
            this.skipAbsolute(1); // \
        } else if (this.isUnicodeEscape()) {
            this.skipAbsolute(5); // \ + 4 hex
        }
        this.absIndex++;
        this.absColumn++;
    }

    private int findAbsColumn() {
        final int s = this.absIndex + 1;
        int i = s;
        for (; i <= s + this.token.offset(); i++) {
            final char c = this.source.charAt(i);
            if (!Character.isWhitespace(c) || c == '\n') {
                return i - s;
            }
        }
        return i - s;
    }

    private boolean requiresEscape() {
        final char c = (char) this.current;
        if (c == '\'') {
            return this.token.stringType() == StringType.SINGLE;
        } else if (c == '"') {
            return this.token.stringType() == StringType.DOUBLE;
        }
        return c == '\n' || c == '\t' || c == '\r' || c == '\f' || c == '\\';
    }

    private boolean isUnicodeEscape() {
        return this.source.charAt(this.absIndex + 1) == 'u'
            && this.source.charAt(this.absIndex) == '\\';
    }

    private void skipAbsolute(final int amount) {
        this.absIndex += amount;
        this.absColumn += amount;
    }

    @Override
    public void close() {}
}
