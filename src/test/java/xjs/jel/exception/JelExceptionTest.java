package xjs.jel.exception;

import org.junit.jupiter.api.Test;
import xjs.serialization.Span;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.SymbolToken;
import xjs.serialization.token.TokenType;
import xjs.serialization.token.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class JelExceptionTest {

    @Test
    public void format_formatsNoLines() {
        final String expected = "JelException: test message";
        final JelException e = new JelException("test message");
        assertEquals(expected, e.format(""));
    }

    @Test
    public void format_formatsSingleLine() {
        final String text = """
            0
            1
            2
            3
            test line
            5
            6
            7
            8
            9""";
        final String expected = """
            JelException: unknown symbols
            ---------------------------------------------------
                4 | test line
                    ^^^^ ^^^^""";
        final JelException e = new JelException("unknown symbols")
            .withSpan(new ParsedToken(8, 12, 4, 0, TokenType.WORD, "test"))
            .withSpan(new ParsedToken(13, 17, 4, 5, TokenType.WORD, "line"));

        assertEquals(expected, e.format(text));
    }

    @Test
    public void format_centersLongLine() {
        final String text = """
            0
            1
            2
            3
            abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890
            5
            6
            7
            8
            9""";
        final String expected = """
            JelException: highway robbery
            -----------------------------------------------------------------------------------
                4 | qrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrs
                              ^        ^""";
        final JelException e = new JelException("highway robbery")
            .withSpan(new SymbolToken(34, 35, 4, 26, TokenType.WORD, '1'))
            .withSpan(new SymbolToken(43, 44, 4, 35, TokenType.WORD, '0'));

        assertEquals(expected, e.format(text));
    }

    @Test
    public void format_formatsMultipleLines() {
        final String text = """
            0
            1
            2
            3
            a b c
            d e f
            g h i
            6
            7
            8
            9""";
        final String expected = """
            JelException: violence
            ---------------------------------------------------
                4 | a b c
                    ^^^
                5 | d e f
                        ^
                6 | g h i
                    ^^^ ^
            ---------------------------------------------------
            To fix this, pick different letters""";
        final JelException e = new JelException("violence")
            .withSpan(new ParsedToken(8, 11, 4, 0, TokenType.WORD, "a b"))
            .withSpan(new ParsedToken(18, 23, 5, 6, 4, TokenType.WORD, "f\ng h"))
            .withSpan(new ParsedToken(24, 25, 6, 4, TokenType.WORD, "i"))
            .withDetails("To fix this, pick different letters");

        assertEquals(expected, e.format(text));
    }

    @Test
    public void format_demoOfStrings() {
        final String text = """
            key: value,
            another: value,
            test token: 'banana',
            another 'test': apples,
            "here you go": '''test here''',
            and another test: 1234,
            yet another test: 5678,
            'okay': 'Im done now'""";
        final String expected = """
            JelException: found some strings
            ---------------------------------------------------
                2 | test token: 'banana',
                                ^^^^^^^^
                3 | another 'test': apples,
                            ^^^^^^
                4 | "here you go": '''test here''',
                    ^^^^^^^^^^^^^  ^^^^^^^^^^^^^^^
                5 | and another test: 1234,
                   \s
                6 | yet another test: 5678,
                   \s
                7 | 'okay': 'Im done now'
                    ^^^^^^  ^^^^^^^^^^^^^""";
        final List<Span<?>> spans =
            Tokenizer.containerize(text).viewTokens()
                .stream()
                .filter(span -> span.type() == TokenType.STRING)
                .collect(Collectors.toList());
        final JelException e =
            new JelException("found some strings").withSpans(spans);

        assertEquals(expected, e.format(text));
    }

    @Test
    public void format_toleratesMultilineSpans() {
        final String text = """
            key: value
            another: value
            here you go:
              '''
              test here
              '''
            and another test: 1234""";
        final String expected = """
            JelException: found a long string
            ---------------------------------------------------
                3 |   '''
                      ^^^
                4 |   test here
                    ^^^^^^^^^^^
                5 |   '''
                    ^^^^^""";
        final List<Span<?>> spans =
            Tokenizer.containerize(text).viewTokens()
                .stream()
                .filter(span -> span.type() == TokenType.STRING && span.lines() > 0)
                .collect(Collectors.toList());
        final JelException e =
            new JelException("found a long string").withSpans(spans);

        assertEquals(expected, e.format(text));
    }

    @Test
    public void format_stretchesSeparator_forLongMessage() {
        final String text = """
            'something to highlight',
            'something else to highlight'""";
        final String expected = """
            JelException: this is a very long message if it keeps going
            -----------------------------------------------------------
                0 | 'something to highlight',
                    ^^^^^^^^^^^^^^^^^^^^^^^^^
                1 | 'something else to highlight'
                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^""";
        final List<Span<?>> spans = new ArrayList<>(
            Tokenizer.containerize(text).viewTokens());
        final JelException e = new JelException("this is a very long message if it keeps going")
            .withSpans(spans);

        assertEquals(expected, e.format(text));
    }

    @Test
    public void format_stretchesSeparator_forLongLines() {
        final String text = "'this message is rather long because it keeps going'";
        final String expected = """
            JelException: this is not a long message
            ------------------------------------------------------------
                0 | 'this message is rather long because it keeps going'
                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^""";
        final List<Span<?>> spans = new ArrayList<>(
                Tokenizer.containerize(text).viewTokens());
        final JelException e = new JelException("this is not a long message")
                .withSpans(spans);

        assertEquals(expected, e.format(text));
    }
}
