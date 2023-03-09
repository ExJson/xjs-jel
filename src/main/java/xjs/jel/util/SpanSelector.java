package xjs.jel.util;

import xjs.serialization.Span;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public final class SpanSelector {
    private static final int LINE_LENGTH = 75;
    private static final int NUM_LENGTH = 5;

    private SpanSelector() {}

    public static String underline(
            final String fullText, final List<Span<?>> spans) {
        final int len = spans.size();
        if (len == 0) {
            return "";
        }
        final int first = spans.get(0).line();
        final int last = spans.get(len - 1).lastLine();
        final int numLines = last - first + 1;
        if (numLines == 1) {
            return underlineSingle(spans, fullText, first);
        }
        return underlineMulti(spans, fullText, first, last);
    }

    private static String underlineSingle(
            final List<Span<?>> spans, final String fullText, final int n) {
        final StringBuilder sb = new StringBuilder();
        final String line = getLines(fullText, n, n).get(0);
        addLine(spans, sb, fullText, line, n);
        return sb.toString();
    }

    private static String underlineMulti(
            final List<Span<?>> spans, final String fullText, final int s, final int e) {
        final StringBuilder sb = new StringBuilder();
        final List<String> lines = getLines(fullText, s, e);
        int n = s;
        for (final String line : lines) {
            addLine(spans, sb, fullText, line, n);
            n++;
        }
        return sb.toString();
    }

    private static List<String> getLines(
            final String fullText, final int s, final int e) {
        return new BufferedReader(new StringReader(fullText)).lines()
            .skip(s)
            .limit(e - s + 1)
            .collect(Collectors.toList());
    }

    private static void addLine(
            final List<Span<?>> spans, final StringBuilder sb,
            final String fullText, final String line, final int n) {
        addLineNumber(sb, n);
        sb.append(" | ");

        final int o;
        if (line.length() > LINE_LENGTH) {
            final int fm = getFirstMarker(spans, n);
            final int lm = getLastMarker(spans, line, n);
            if (lm - fm > LINE_LENGTH) {
                o = fm;
            } else {
                o = Math.max(0, fm - 18);
            }
            final int e = Math.min(line.length(), o + LINE_LENGTH);
            sb.append(line, o, e);
        } else {
            o = 0;
            sb.append(line);
        }

        sb.append('\n');
        addUnderline(spans, sb, fullText, line, n, o);
    }

    private static void addLineNumber(final StringBuilder sb, final int n) {
        final String sn = String.valueOf(n);
        final int spaces = NUM_LENGTH - sn.length();
        for (int i = 0; i < spaces; i++) {
            sb.append(' ');
        }
        if (spaces < 0) {
            sb.append(sn, -spaces, sn.length());
        } else {
            sb.append(sn);
        }
    }

    private static int getFirstMarker(final List<Span<?>> spans, final int n) {
        for (final Span<?> span : spans) { // sorted
            if (span.line() == n) {
                return span.start();
            }
        }
        return 0;
    }

    private static int getLastMarker(
            final List<Span<?>> spans, final String line, final int n) {
        for (int i = spans.size() - 1; i >= 0; i--) {
            final Span<?> span = spans.get(i);
            if (span.line() == n) {
                if (span.lines() == 0) {
                    return span.offset() + span.length();
                }
                return line.length();
            }
        }
        return line.length();
    }

    private static void addUnderline(
            final List<Span<?>> spans, final StringBuilder sb,
            final String fullText, final String line, final int n, final int o) {
        final BitSet underline = new BitSet(LINE_LENGTH);
        for (final Span<?> span : spans) {
            if (span.start() < 0) {
                continue;
            }
            final int offset = span.offset() - o;
            if (offset < 0 || offset >= LINE_LENGTH) {
                continue;
            }
            if (span.line() == n) {
                final int len = span.length();
                for (int i = offset; i < offset + len && i < line.length(); i++) {
                    underline.set(i);
                }
            } else if (span.line() < n && span.lastLine() > n) {
                for (int i = 0; i < line.length(); i++) {
                    underline.set(i);
                }
            } else if (span.line() < n && span.lastLine() == n) {
                final int end = getEndOffset(fullText, span);
                for (int i = 0; i < end; i++) {
                    underline.set(i);
                }
            }
        }
        if (underline.length() > 0) {
            for (int i = 0; i < NUM_LENGTH + 3; i++) {
                sb.append(' ');
            }
            int i = 0;
            while (i < underline.length()) {
                final int end = underline.nextSetBit(i);
                for (int j = i; j < end; j++) {
                    sb.append(' ');
                }
                sb.append('^');
                i = end + 1;
            }
            sb.append('\n');
        }
    }

    private static int getEndOffset(final String fullText, final Span<?> span) {
        int o = span.offset();
        for (int i = span.start(); i < span.end(); i++) {
            if (fullText.charAt(i) == '\n') {
                o = 0;
            } else {
                o++;
            }
        }
        return o;
    }
}
