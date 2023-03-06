package xjs.jel.util;

import xjs.exception.SyntaxException;
import xjs.serialization.Span;

public final class SpanUtils {

    public static Span<?> fromSyntaxException(
            final String fullText, final SyntaxException e) {
        final int s = getIndex(fullText, e);
        final int l = e.getLine();
        final int o = e.getColumn();
        return new Span<Void>(s, s + 1, l, o, null);
    }

    private static int getIndex(
            final String fullText, final SyntaxException e) {
        final int l = indexOfLine(fullText, e.getLine());
        if (l < 0) {
            return l;
        }
        return l + e.getColumn();
    }

    private static int indexOfLine(final String s, int l) {
        if (l == 0) {
            return 0;
        }
        int idx = s.indexOf('\n');
        while (idx != -1 && --l > 0) {
            idx = s.indexOf('\n', idx + 1);
        }
        return idx;
    }
}
