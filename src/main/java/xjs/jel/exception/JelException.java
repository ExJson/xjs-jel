package xjs.jel.exception;

import org.jetbrains.annotations.Nullable;
import xjs.exception.SyntaxException;
import xjs.jel.JelContext;
import xjs.jel.util.SpanMap;
import xjs.jel.util.SpanSelector;
import xjs.jel.util.SpanUtils;
import xjs.serialization.Span;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class JelException extends Exception {
    private static final int MIN_SEPARATOR_LEN = 51;
    private static final int MAX_SEPARATOR_LEN = 83;

    private final SpanMap spanMap = new SpanMap();
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
        return this.withSpan((String) null, span);
    }

    public JelException withSpans(final Span<?>... spans) {
        return this.withSpans((String) null, spans);
    }

    public JelException withSpans(final Iterable<Span<?>> spans) {
        return this.withSpans((String) null, spans);
    }

    public JelException withSpan(final JelContext ctx, final Span<?> span) {
        return this.withSpan(ctx.getScope().getFilePath(), span);
    }

    public JelException withSpans(final JelContext ctx, final Span<?>... spans) {
        return this.withSpans(ctx.getScope().getFilePath(), spans);
    }

    public JelException withSpans(final JelContext ctx, final Iterable<Span<?>> spans) {
        return this.withSpans(ctx.getScope().getFilePath(), spans);
    }

    public JelException withSpan(
            final String path, final Span<?> span) {
        this.spanMap.add(path, span);
        return this;
    }

    public JelException withSpans(
            final String path, final Span<?>... spans) {
        this.spanMap.add(path, spans);
        return this;
    }

    public JelException withSpans(
            final String path, final Iterable<Span<?>> spans) {
        this.spanMap.add(path, spans);
        return this;
    }

    public JelException remapSpans(final String path) {
        final List<Span<?>> spans = this.spanMap.remove(null);
        if (spans != null) {
            this.spanMap.put(path, spans);
        }
        return this;
    }

    public JelException withDetails(final String details) {
        this.details = details;
        return this;
    }

    public SpanMap getSpans() {
        return this.spanMap;
    }

    public @Nullable String getDetails() {
        return this.details;
    }

    public String format(final String fullText) {
        return this.format(null, fullText);
    }

    public String format(final @Nullable JelContext ctx, final String fullText) {
        final StringBuilder sb = new StringBuilder();
        this.addHeader(sb);

        final Map<String, String> lines = this.buildLines(ctx, fullText);
        final int separatorLen = this.getSeparatorLen(lines);

        lines.forEach((path, underlined) -> {
            if (underlined.length() > 0) {
                if (path != null) {
                    this.addSeparator(sb, separatorLen);
                    sb.append("In file: ");
                    sb.append(this.getRelativePath(ctx, path));
                }
                this.addSeparator(sb, separatorLen);
                sb.append(underlined, 0, underlined.length() - 1);
            }
        });
        if (this.details != null) {
            this.addSeparator(sb, separatorLen);
            sb.append(this.details);
        }
        return sb.toString();
    }

    private Map<String, String> buildLines(final JelContext ctx, final String fullText) {
        final Map<String, String> lines = new HashMap<>();
        this.spanMap.forEach((path, spans) -> {
            String text = this.getFullText(ctx, path);
            if (text == null) {
                if (path != null) {
                    return; // given text does not correspond
                }
                text = fullText;
            }
            spans.sort(Span::compareTo);
            lines.put(path, SpanSelector.underline(text, spans));
        });
        return lines;
    }

    private String getFullText(final JelContext ctx, final String path) {
        return ctx != null ? ctx.getFullText(path) : null;
    }

    private String getRelativePath(final JelContext ctx, final String path) {
        return ctx != null ? ctx.getRelativePath(path) : new File(path).getName();
    }

    private int getSeparatorLen(final Map<String, String> lines) {
        final int headerLen =
            this.getClass().getSimpleName().length()
                + this.getLocalizedMessage().length() + 2;
        int linesLen = 0;
        for (final String underlined : lines.values()) {
            linesLen = this.getLongestLine(underlined);
        }
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
