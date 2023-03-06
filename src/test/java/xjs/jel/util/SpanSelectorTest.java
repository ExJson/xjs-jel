package xjs.jel.util;

import org.junit.jupiter.api.Test;
import xjs.serialization.Span;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;
import xjs.serialization.token.Tokenizer;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static xjs.jel.util.SpanSelector.underline;

public final class SpanSelectorTest {

    @Test
    public void underline_selectsSingleLine() {
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
                4 | test line
                    ^^^^ ^^^^
            """;
        final List<Span<?>> spans = List.of(
            new ParsedToken(8, 12, 4, 0, TokenType.WORD, "test"),
            new ParsedToken(13, 17, 4, 5, TokenType.WORD, "line"));

        assertEquals(expected, underline(text, spans));
    }

    @Test
    public void underline_selectsMultipleLines() {
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
                4 | a b c
                    ^^^
                5 | d e f
                        ^
                6 | g h i
                    ^^^ ^
            """;
        final List<Span<?>> spans = List.of(
            new ParsedToken(8, 11, 4, 0, TokenType.WORD, "a b"),
            new ParsedToken(18, 23, 5, 6, 4, TokenType.WORD, "f\ng h"),
            new ParsedToken(24, 25, 6, 4, TokenType.WORD, "i"));

        assertEquals(expected, underline(text, spans));
    }

    @Test
    public void underline_demoOfStrings() {
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
                    ^^^^^^  ^^^^^^^^^^^^^
            """;
        final List<Span<?>> spans =
            Tokenizer.containerize(text).viewTokens()
                .stream()
                .filter(span -> span.type() == TokenType.STRING)
                .collect(Collectors.toList());

        assertEquals(expected, underline(text, spans));
    }

    @Test
    public void underline_toleratesMultilineSpans() {
        final String text = """
            key: value
            another: value
            here you go:
              '''
              test here
              '''
            and another test: 1234""";
        final String expected = """
                3 |   '''
                      ^^^
                4 |   test here
                    ^^^^^^^^^^^
                5 |   '''
                    ^^^^^
            """;
        final List<Span<?>> spans =
            Tokenizer.containerize(text).viewTokens()
                .stream()
                .filter(span -> span.type() == TokenType.STRING && span.lines() > 0)
                .collect(Collectors.toList());

        assertEquals(expected, underline(text, spans));
    }
}
