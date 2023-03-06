package xjs.jel.exception;

import org.jetbrains.annotations.Nullable;
import xjs.exception.SyntaxException;
import xjs.jel.util.SpanSelector;
import xjs.jel.util.SpanUtils;
import xjs.serialization.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class JelException extends Exception {
    private static final int MIN_SEPARATOR_LEN = 51;
    private static final int MAX_SEPARATOR_LEN = 83;

    private final List<Span<?>> spans = new ArrayList<>();
    private @Nullable String details;

    public JelException(final String msg) {
        super(msg);
    }

    public JelException(final Throwable cause) {
        super(cause);
    }

    public JelException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    public static JelException fromSyntaxException(
            final String fullText, final SyntaxException e) {
        return new JelException(e.getMessage(), e)
            .withSpan(SpanUtils.fromSyntaxException(fullText, e));
    }

    public JelException withSpan(final Span<?> span) {
        Objects.requireNonNull(span, "span");
        this.spans.add(span);
        return this;
    }

    public JelException withSpans(final Span<?>... spans) {
        for (final Span<?> span : spans) {
            this.withSpan(span);
        }
        return this;
    }

    public JelException withSpans(final Iterable<Span<?>> spans) {
        spans.forEach(this.spans::add);
        return this;
    }

    public JelException withDetails(final String details) {
        this.details = details;
        return this;
    }

    public List<Span<?>> getSpans() {
        return this.spans;
    }

    public @Nullable String getDetails() {
        return this.details;
    }

    public String format(final String fullText) {
        this.spans.sort(Span::compareTo);
        final String lines =
            SpanSelector.underline(fullText, this.spans);
        final int separatorLen =
            this.getSeparatorLen(lines);

        final StringBuilder sb = new StringBuilder();
        this.addHeader(sb);
        if (lines.length() > 0) {
            this.addSeparator(sb, separatorLen);
            sb.append(lines, 0, lines.length() - 1);
        }
        if (this.details != null) {
            this.addSeparator(sb, separatorLen);
            sb.append(this.details);
        }
        return sb.toString();
    }

    private int getSeparatorLen(final String lines) {
        final int headerLen =
            this.getClass().getSimpleName().length()
                + this.getLocalizedMessage().length() + 2;
        final int linesLen = this.getLongestLine(lines);
        final int detailsLen =
            this.details != null ? this.details.length() : 0;

        final int maxLen = IntStream.of(
            MIN_SEPARATOR_LEN, headerLen, linesLen, detailsLen)
                .max().getAsInt();
        return Math.min(MAX_SEPARATOR_LEN, maxLen);
    }

    private int getLongestLine(final String lines) {
        int longest = 0;
        int offset = 0;
        for (int i = 0; i < lines.length(); i++) {
            if (lines.charAt(i) == '\n') {
                longest = Math.max(longest, offset);
                offset = 0;
            } else {
                offset++;
            }
        }
        return longest;
    }

    private void addHeader(final StringBuilder sb) {
        sb.append(this.getClass().getSimpleName());
        sb.append(": ");
        sb.append(this.getLocalizedMessage());
    }

    private void addSeparator(final StringBuilder sb, final int len) {
        sb.append('\n');
        for (int i = 0; i < len; i++) {
            sb.append('-');
        }
        sb.append('\n');
    }
}
