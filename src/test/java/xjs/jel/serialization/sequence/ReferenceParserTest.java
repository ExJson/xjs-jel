package xjs.jel.serialization.sequence;

import org.junit.jupiter.api.Test;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.path.CallComponent;
import xjs.jel.path.IndexComponent;
import xjs.jel.path.IndexRangeComponent;
import xjs.jel.path.KeyComponent;
import xjs.jel.sequence.JelType;
import xjs.serialization.token.Token;
import xjs.serialization.token.Tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ReferenceParserTest {

    private static final ReferenceParser PARSER = Sequencer.JEL.referenceParser;

    @Test
    public void parse_readsKeyComponent() throws JelException {
        final ReferenceExpression exp = parse("$key");

        assertEquals(1, exp.spans().size());

        final KeyComponent key = (KeyComponent) exp.spans().get(0);
        assertEquals("key", ((Token) key.spans().get(0)).parsed());
    }

    @Test
    public void parse_readsDottedKeyComponents_asChain() throws JelException {
        final ReferenceExpression exp = parse("$key.one.two");

        assertEquals(3, exp.spans().size());

        final KeyComponent key = (KeyComponent) exp.spans().get(0);
        assertEquals("key", ((Token) key.spans().get(0)).parsed());

        final KeyComponent one = (KeyComponent) exp.spans().get(1);
        assertEquals("one", ((Token) one.spans().get(0)).parsed());

        final KeyComponent two = (KeyComponent) exp.spans().get(2);
        assertEquals("two", ((Token) two.spans().get(0)).parsed());
    }

    @Test
    public void parse_readsStringInPath_asKeyComponent() throws JelException {
        final ReferenceExpression exp = parse("$one.'two and three'");

        assertEquals(2, exp.spans().size());

        final KeyComponent one = (KeyComponent) exp.spans().get(0);
        assertEquals("one", ((Token) one.spans().get(0)).parsed());

        final KeyComponent twoAndThree = (KeyComponent) exp.spans().get(1);
        assertEquals("two and three", ((Token) twoAndThree.spans().get(0)).parsed());
    }

    @Test
    public void parse_readsCallComponent() throws JelException {
        final ReferenceExpression exp = parse("$call()");

        assertEquals(1, exp.spans().size());

        final CallComponent call = (CallComponent) exp.spans().get(0);
        assertEquals("call", ((Token) call.spans().get(0)).parsed());
    }

    @Test
    public void parse_readsDottedCallComponents_asChain() throws JelException {
        final ReferenceExpression exp = parse("$call().one().two()");

        assertEquals(3, exp.spans().size());

        final CallComponent call = (CallComponent) exp.spans().get(0);
        assertEquals("call", ((Token) call.spans().get(0)).parsed());

        final CallComponent one = (CallComponent) exp.spans().get(1);
        assertEquals("one", ((Token) one.spans().get(0)).parsed());

        final CallComponent two = (CallComponent) exp.spans().get(2);
        assertEquals("two", ((Token) two.spans().get(0)).parsed());
    }

    @Test
    public void parse_readsIndexComponent() throws JelException {
        final ReferenceExpression exp = parse("$[0]");

        assertEquals(1, exp.spans().size());

        final IndexComponent index = (IndexComponent) exp.spans().get(0);
        assertEquals(0, index.index);
    }

    @Test
    public void parse_readsChainedIndexComponents() throws JelException {
        final ReferenceExpression exp = parse("$[1][2]");

        assertEquals(2, exp.spans().size());

        final IndexComponent one = (IndexComponent) exp.spans().get(0);
        assertEquals(1, one.index);

        final IndexComponent two = (IndexComponent) exp.spans().get(1);
        assertEquals(2, two.index);
    }

    @Test
    public void parse_readsIndexRangeComponent() throws JelException {
        final ReferenceExpression exp = parse("$[1:2]");

        assertEquals(1, exp.spans().size());

        final IndexRangeComponent range = (IndexRangeComponent) exp.spans().get(0);
        assertEquals(1, range.s);
        assertEquals(2, range.e);
    }

    @Test
    public void parse_readsIndexRangeComponent_withNoFirstNumber() throws JelException {
        final ReferenceExpression exp = parse("$[:1]");

        assertEquals(1, exp.spans().size());

        final IndexRangeComponent range = (IndexRangeComponent) exp.spans().get(0);
        assertEquals(0, range.s);
        assertEquals(1, range.e);
    }

    @Test
    public void parse_readsIndexRangeComponent_withNoSecondNumber() throws JelException {
        final ReferenceExpression exp = parse("$[3:]");

        assertEquals(1, exp.spans().size());

        final IndexRangeComponent range = (IndexRangeComponent) exp.spans().get(0);
        assertEquals(3, range.s);
        assertEquals(-1, range.e);
    }

    @Test
    public void parse_readsIndexRangeComponent_withNoNumbers() throws JelException {
        final ReferenceExpression exp = parse("$[:]");

        assertEquals(1, exp.spans().size());

        final IndexRangeComponent range = (IndexRangeComponent) exp.spans().get(0);
        assertEquals(0, range.s);
        assertEquals(-1, range.e);
    }

    @Test
    public void parse_doesNotTolerate_emptyBrackets() {
        final String text = "$[]";
        final String message = """
            JelException: Empty index component
            ---------------------------------------------------
                1 | $[]
                     ^^
            ---------------------------------------------------
            Hint: should be an index or index range component""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parse_doesNotTolerate_extraneousTokensInBrackets() {
        final String text = "$[1 &]";
        final String message = """
            JelException: Unexpected tokens after index range
            --------------------------------------------------------------------
                1 | $[1 &]
                        ^
            --------------------------------------------------------------------
            Hint: expression should be [s:e], where s and e are optional numbers""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parse_doesNotTolerate_duplicateRangeOperator() {
        final String text = "$[1::2]";
        final String message = """
            JelException: Redundant range operator
            --------------------------------------------------------------------
                1 | $[1::2]
                       ^^
            --------------------------------------------------------------------
            Hint: expression should be [s:e], where s and e are optional numbers""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parse_toleratesWhitespaceAndComments_inBrackets() throws JelException {
        final ReferenceExpression exp = parse("$[\n0//comment\n]");

        assertEquals(1, exp.spans().size());

        final IndexComponent index = (IndexComponent) exp.spans().get(0);
        assertEquals(0, index.index);
    }

    @Test
    public void parse_readsChainedComponents_ofMixedType() throws JelException {
        final ReferenceExpression exp = parse("$call().one.two()");

        assertEquals(3, exp.spans().size());

        final CallComponent call = (CallComponent) exp.spans().get(0);
        assertEquals("call", ((Token) call.spans().get(0)).parsed());

        final KeyComponent one = (KeyComponent) exp.spans().get(1);
        assertEquals("one", ((Token) one.spans().get(0)).parsed());

        final CallComponent two = (CallComponent) exp.spans().get(2);
        assertEquals("two", ((Token) two.spans().get(0)).parsed());
    }

    @Test
    public void parse_endsAtFirstWhitespace() throws JelException {
        final ReferenceExpression exp = parse("$one.two three");

        assertEquals(2, exp.spans().size());

        final KeyComponent one = (KeyComponent) exp.spans().get(0);
        assertEquals("one", ((Token) one.spans().get(0)).parsed());

        final KeyComponent two = (KeyComponent) exp.spans().get(1);
        assertEquals("two", ((Token) two.spans().get(0)).parsed());
    }

    @Test
    public void parse_doesNotTolerate_trailingDot() {
        final String text = "$one.";
        final String message = """
            JelException: expected key
            ------------------------------------------------------------------
                1 | $one.
                        ^
            ------------------------------------------------------------------
            Hint: the dot accessor indicates that a word or string will follow""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parse_doesNotTolerate_missingKey_inDottedPath() {
        final String text = "$one.@";
        final String message = """
            JelException: expected key
            ------------------------------------------------------------------
                1 | $one.@
                        ^
            ------------------------------------------------------------------
            Hint: the dot accessor indicates that a word or string will follow""";
        final JelException e =
            assertThrows(JelException.class, () -> parse(text));
        assertEquals(message, e.format(text));
    }

    @Test
    public void parse_readsTrailingDoubleDot_asReferenceExpansion() throws JelException {
        final ReferenceExpression exp = parse("$key.one.two..");

        assertEquals(3, exp.spans().size());
        assertEquals(JelType.REFERENCE_EXPANSION, exp.type());
    }

    @Test
    public void parse_doesNotRead_afterExpansion() throws JelException {
        final ReferenceExpression exp = parse("$key..one");

        assertEquals(1, exp.spans().size());

        final KeyComponent key = (KeyComponent) exp.spans().get(0);
        assertEquals("key", ((Token) key.spans().get(0)).parsed());
    }

    private static ReferenceExpression parse(final String text) throws JelException {
        return PARSER.parse(Tokenizer.containerize(text).iterator());
    }
}
