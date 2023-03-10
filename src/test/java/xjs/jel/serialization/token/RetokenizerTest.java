package xjs.jel.serialization.token;

import org.junit.jupiter.api.Test;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.SymbolToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;
import xjs.serialization.token.Tokenizer;
import xjs.serialization.util.StringContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RetokenizerTest {

    @Test
    public void inspectValue_withNoExpressions_returnsOriginalToken() {
        final String input = "'hello, world'";
        assertEquals(List.of(token(input)), inspectValue(input));
    }

    @Test
    public void inspectValue_withPathExpression_returnsInlineTokens() {
        final List<Token> tokens = inspectValue("'$path[0].to'");

        assertEquals(5, tokens.size());

        final Token first = tokens.get(0);
        assertTrue(first.isSymbol('$'));

        final Token second = tokens.get(1);
        assertEquals("path", second.parsed());

        final Token third = tokens.get(2);
        assertEquals(TokenType.BRACKETS, third.type());

        final Token insideOf3 = ((ContainerToken) third).get(0);
        assertEquals(0, ((NumberToken) insideOf3).number);

        final Token fourth = tokens.get(3);
        assertTrue(fourth.isSymbol('.'));

        final Token fifth = tokens.get(4);
        assertEquals("to", fifth.parsed());
    }

    @Test
    public void inspectValue_containingPaths_returnsPathsAndStrings() {
        final List<Token> tokens = inspectValue("'hello $mom and dad'");

        assertEquals(4, tokens.size());
        assertEquals("hello ", tokens.get(0).parsed());
        assertTrue(tokens.get(1).isSymbol('$'));
        assertEquals("mom", tokens.get(2).parsed());
        assertEquals(" and dad", tokens.get(3).parsed());
    }

    @Test
    public void inspectValue_whenPathIsSurroundedByQuotes_parsesQuotesAsText() {
        final List<Token> tokens = inspectValue("'''hello '$mom' and dad'''");

        assertEquals(4, tokens.size());
        assertEquals("hello '", tokens.get(0).parsed());
        assertTrue(tokens.get(1).isSymbol('$'));
        assertEquals("mom", tokens.get(2).parsed());
        assertEquals("' and dad", tokens.get(3).parsed());
    }

    @Test
    public void inspectValue_preservesSurroundingWhitespace() {
        final List<Token> tokens = inspectValue("'  $ref  '");

        assertEquals(4, tokens.size());
        assertEquals("  ", tokens.get(0).parsed());
        assertTrue(tokens.get(1).isSymbol('$'));
        assertEquals("ref", tokens.get(2).parsed());
        assertEquals("  ", tokens.get(3).parsed());
    }

    @Test
    public void inspectValue_parsesNumbersAndOperators_asNonTextTokens() {
        final List<Token> tokens = inspectValue("'1234 + 5678'");

        assertEquals(3, tokens.size());
        assertEquals(1234, ((NumberToken) tokens.get(0)).number);
        assertTrue(tokens.get(1).isSymbol('+'));
        assertEquals(5678, ((NumberToken) tokens.get(2)).number);
    }

    @Test
    public void inspectKey_withoutAliasOperator_returnsOriginalToken() {
        final String text = "'1234 + 5678'";
        assertEquals(List.of(token(text)), inspectKey(text));
    }

    @Test
    public void inspectKey_withAliasOperator_parsesFullTextNormally() {
        final List<Token> tokens = inspectKey("'abc >> 123'");

        assertEquals(4, tokens.size());
        assertEquals(TokenType.WORD, tokens.get(0).type());
        assertTrue(tokens.get(1).isSymbol('>'));
        assertTrue(tokens.get(2).isSymbol('>'));
        assertEquals(123, ((NumberToken) tokens.get(3)).number);
    }

    @Test
    public void inspectValue_whenNotPathing_doesNotParseContainers_asSymbols() {
        final List<Token> tokens = inspectValue("' ( 1234 '");

        assertEquals(1, tokens.size());
        assertEquals(TokenType.STRING, tokens.get(0).type());
    }

    @Test
    public void inspectValue_whenContainersAreValidArithmetic_parseContainers_asSymbols() {
        final List<Token> tokens = inspectValue("' ( 1234 + 5678 ) '");

        assertEquals(1, tokens.size());
        assertEquals(TokenType.PARENTHESES, tokens.get(0).type());

        final ContainerToken math = (ContainerToken) tokens.get(0);
        assertEquals(3, math.size());
        assertEquals(1234, ((NumberToken) math.get(0)).number);
        assertEquals('+', ((SymbolToken) math.get(1)).symbol);
        assertEquals(5678, ((NumberToken) math.get(2)).number);
    }

    private static List<Token> inspectValue(final String text) {
        return inspect(text, StringContext.VALUE);
    }

    private static List<Token> inspectKey(final String text) {
        return inspect(text, StringContext.KEY);
    }

    private static List<Token> inspect(final String text, final StringContext ctx) {
        return Retokenizer.inspect(text, token(text), ctx).viewTokens();
    }

    private static StringToken token(final String text) {
        return (StringToken) Tokenizer.containerize(text).get(0);
    }
}
