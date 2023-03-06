package xjs.jel.serialization.util;

import org.junit.jupiter.api.Test;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.Tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RereaderTest {

    @Test
    public void reader_withSimpleQuotedString_tracksAbsoluteIndices() {
        final Rereader reader = sample("'test'");

        assertEquals('t', reader.current);
        assertEquals(0, reader.index);
        assertEquals(1, reader.absIndex);
        assertEquals(0, reader.column);
        assertEquals(1, reader.absColumn);
        assertEquals(0, reader.line);
        assertEquals(0, reader.absLine);

        assertEquals('e', read(reader));
        assertEquals('s', read(reader));
        assertEquals('t', read(reader));
        assertEquals(-1, read(reader));
        assertEquals(5, reader.absIndex);
        assertEquals(5, reader.absColumn);
        assertEquals(0, reader.absLine);
    }

    @Test // hard to follow, seems intrinsic
    public void reader_withQuotedString_accountsForEscapeSequences() {
        // 'a\nb\tc\\d' == a (nl) b (tab) c \ d
        final Rereader reader = sample("'a\\nb\\tc\\\\d'");

        assertEquals('a', reader.current);
        assertEquals(1, reader.absIndex);

        assertEquals('\n', read(reader));
        assertEquals(2, reader.absIndex);

        assertEquals('b', read(reader));
        assertEquals(4, reader.absIndex);

        assertEquals('\t', read(reader));
        assertEquals(5, reader.absIndex);

        assertEquals('c', read(reader));
        assertEquals(7, reader.absIndex);

        assertEquals('\\', read(reader));
        assertEquals(8, reader.absIndex);

        assertEquals('d', read(reader));
        assertEquals(10, reader.absIndex);

        assertEquals(-1, read(reader));
        assertEquals(11, reader.absIndex);
    }

    @Test
    public void reader_withQuotedString_accountsForUnicodeEscapes() {
        // [ '\u1234', 'Y' ]
        final Rereader reader = sample("'\\u1234Y'");

        assertEquals('\u1234', reader.current);
        assertEquals(1, reader.absIndex);
        assertEquals(0, reader.index);

        assertEquals('Y', read(reader));
        assertEquals(7, reader.absIndex);
        assertEquals(1, reader.index);

        assertEquals(-1, read(reader));
        assertEquals(8, reader.absIndex);
        assertEquals(2, reader.index);
    }

    @Test
    public void reader_withSimpleMultiString_tracksAbsoluteIndices() {
        final Rereader reader = sample("'''test'''");

        assertEquals('t', reader.current);
        assertEquals(0, reader.index);
        assertEquals(3, reader.absIndex);
        assertEquals(0, reader.column);
        assertEquals(3, reader.absColumn);
        assertEquals(0, reader.line);
        assertEquals(0, reader.absLine);

        assertEquals('e', read(reader));
        assertEquals('s', read(reader));
        assertEquals('t', read(reader));
        assertEquals(-1, read(reader));
        assertEquals(7, reader.absIndex);
        assertEquals(7, reader.absColumn);
        assertEquals(0, reader.absLine);
    }

    @Test
    public void reader_withMultiString_accountsForInitialWhitespace() {
        // "test"
        final Rereader reader = sample("'''  \ntest'''");

        assertEquals('t', reader.current);
        assertEquals(0, reader.index);
        assertEquals(6, reader.absIndex);
        assertEquals(0, reader.column);
        assertEquals(0, reader.absColumn);
        assertEquals(0, reader.line);
        assertEquals(1, reader.absLine);

        assertEquals('e', read(reader));
        assertEquals('s', read(reader));
        assertEquals('t', read(reader));
        assertEquals(-1, read(reader));
        assertEquals(10, reader.absIndex);
        assertEquals(4, reader.absColumn);
        assertEquals(1, reader.absLine);
    }

    @Test
    public void reader_withMultiString_accountsForUnusualOffset() {
        // "a\nb\nc"
        final Rereader reader = sample("  '''\n a\n  b\nc\n  '''");

        assertEquals('a', reader.current);
        assertEquals(0, reader.index);
        assertEquals(7, reader.absIndex);
        assertEquals(0, reader.column);
        assertEquals(1, reader.absColumn);
        assertEquals(0, reader.line);
        assertEquals(1, reader.absLine);

        assertEquals('\n', read(reader));
        assertEquals('b', read(reader));
        assertEquals(11, reader.absIndex);
        assertEquals(2, reader.absColumn);
        assertEquals(2, reader.absLine);

        assertEquals('\n', read(reader));
        assertEquals('c', read(reader));
        assertEquals(13, reader.absIndex);
        assertEquals(0, reader.absColumn);
        assertEquals(3, reader.absLine);

        assertEquals(-1, read(reader));
        assertEquals(14, reader.absIndex);
        assertEquals(1, reader.absColumn);
        assertEquals(3, reader.absLine);
    }

    private static Rereader sample(final String text) {
        return new Rereader(text, token(text));
    }

    private static StringToken token(final String text) {
        return (StringToken) Tokenizer.containerize(text).get(0);
    }

    private static int read(final Rereader reader) {
        reader.read();
        return reader.current;
    }
}
