package xjs.jel.serialization.sequence;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ArithmeticExpression;
import xjs.jel.expression.BooleanExpression;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.expression.OperatorExpression;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.sequence.ModifyingOperator;
import xjs.jel.sequence.ModifyingOperatorSequence;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class OperatorParserTest {

    private static final OperatorParser PARSER = Sequencer.JEL.operatorParser;

    @Test
    public void simpleBinomial_isExpression() {
        assertTrue(isExpression("1 + 2"));
    }

    @Test
    public void binomial_withComments_isExpression() {
        assertTrue(isExpression("/*c*/ 1 /*c*/ + 2"));
    }

    @Test
    public void binomial_withNewlines_isExpression() {
        assertTrue(isExpression("1 \n + 2"));
    }

    @Test
    public void binomial_withMultipleLhsTokens_isNotExpression() {
        assertFalse(isExpression("1 /*c*/ 2 + 3"));
    }

    @Test
    public void binomialExpression_mayStartWithString() {
        assertTrue(isExpression("'str' + 'str'"));
    }

    @Test
    public void binomial_withMissingOperand_isNotExpression() {
        assertFalse(isExpression("1 +"));
    }

    @Test
    public void operand_withMultipleTrailingOperators_isIllegalExpression() {
        assertTrue(isExpression("1 + +")); // pattern matches, parse fails
    }

    @Test
    public void operand_withLeadingOperator_isNotExpression() {
        assertFalse(isExpression("+ 2"));
    }

    @Test
    public void isExpression_whenFirstOperandIsParenthetical_checksRecursively() {
        assertTrue(isExpression("((1 + 2))"));
        assertFalse(isExpression("((1))"));
    }

    @Test
    public void binomial_whenLhsIsEmptyParentheses_isIllegalExpression() {
        assertTrue(isExpression("() + 2")); // pattern matches, parse fails
    }

    @Test
    public void binomial_whenLhsIsReference_isExpression() {
        assertTrue(isExpression("$path.to.ex + 1"));
    }

    @Test
    public void binomial_whenLhsIsCallWithSpaces_isExpression() {
        assertTrue(isExpression("$call(1, 2, 3) + 3"));
    }

    @Test
    public void binomial_withAdditionalTokensAfterReference_isNotExpression() {
        assertFalse(isExpression("$path 2 3 + 4"));
    }

    @Test
    public void binomial_withIllegalReference_isExpression() {
        assertTrue(isExpression("$path.1 + 2"));
    }

    @Test
    public void singleNumber_isNotExpression() {
        assertFalse(isExpression("3"));
    }

    @Test
    public void binomial_withSimpleRelationalOperator_isExpression() {
        assertTrue(isExpression("$a < $b"));
    }

    @Test
    public void tryParseDefaultCase_whenPatternDoesNotMatch_returnsNull() throws JelException {
        assertNull(tryParseDefaultCase("1234"));
    }

    @Test
    public void tryParseDefaultCase_withAdditionalTokens_returnsNull() throws JelException {
        assertNull(tryParseDefaultCase("__"));
    }

    @Test
    public void tryParseDefaultCase_whenPatternMatches_returnsExpression() throws JelException {
        final BooleanExpression exp = tryParseDefaultCase("_");

        assertNotNull(exp);
        assertEquals(1, exp.size());
        assertTrue(exp.applyAsBoolean(null));
    }

    @Test
    public void tryParseDefaultCase_toleratesWhitespace() throws JelException {
        final BooleanExpression exp = tryParseDefaultCase("/*c*/ _ /*c*/");

        assertNotNull(exp);
        assertEquals(1, exp.size());
        assertTrue(exp.applyAsBoolean(null));
    }

    @Test
    public void parse_withOnlyNumber_isArithmeticExpression() throws JelException {
        final OperatorExpression exp = parse("1234");

        assertInstanceOf(ArithmeticExpression.class, exp);
        assertEquals(1, exp.size());
        final Expression n = assertInstanceOf(LiteralExpression.OfNumber.class, exp.spans().get(0));
        assertEquals(1234, n.applyAsNumber(null));
    }

    @Test
    public void parse_withNoOperators_andNotANumberOrBoolean_isOperatorExpression() throws JelException {
        final OperatorExpression exp = parse("'str'");

        assertInstanceOf(OperatorExpression.class, exp);
        assertEquals(1, exp.size());
        final Expression s = assertInstanceOf(LiteralExpression.OfString.class, exp.spans().get(0));
        assertEquals("str", s.applyAsString(null));
    }

    @Test
    public void parse_withNoOperators_doesParseInvertModifier() throws JelException {
        final OperatorExpression exp = parse("- 1");

        assertInstanceOf(ArithmeticExpression.class, exp);
        assertEquals(2, exp.size());
        final ModifyingOperatorSequence m =
            assertInstanceOf(ModifyingOperatorSequence.class, exp.spans().get(0));
        assertEquals(ModifyingOperator.INVERT, m.op);
        final Expression n =
            assertInstanceOf(LiteralExpression.OfNumber.class, exp.spans().get(1));
        assertEquals(1, n.applyAsNumber(null));
    }

    @Test
    public void parse_withNoOperators_doesParseNotModifier() throws JelException {
        final OperatorExpression exp = parse("!$ref");

        assertInstanceOf(BooleanExpression.class, exp);
        assertEquals(2, exp.size());
        final ModifyingOperatorSequence m =
            assertInstanceOf(ModifyingOperatorSequence.class, exp.spans().get(0));
        assertEquals(ModifyingOperator.NOT, m.op);
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(1));
    }

    @Test
    public void parse_withInvertModifier_whenNumberIsAlreadyNegative_throwsException() {
        assertThrows(JelException.class, () -> parse("1 + - -4"));
    }

    @Test
    public void parse_whenModifierIsDangling_throwsException() {
        assertThrows(JelException.class, () -> parse("true && !"));
    }

    @Test
    public void parse_whenOperatorIsDangling_throwsException() {
        assertThrows(JelException.class, () -> parse("1 +"));
    }

    @Test
    public void parse_readsBinomialArithmeticExpression() throws JelException {
        final OperatorExpression exp = parse("1 + 2");

        assertInstanceOf(ArithmeticExpression.class, exp);
        assertEquals(3, exp.size());
        final Expression n1 =
            assertInstanceOf(LiteralExpression.class, exp.spans().get(0));
        assertEquals(1, n1.applyAsNumber(null));
        final OperatorSequence o =
            assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertEquals(Operator.ADD, o.op);
        final Expression n2 =
            assertInstanceOf(LiteralExpression.class, exp.spans().get(2));
        assertEquals(2, n2.applyAsNumber(null));
    }

    @Test
    public void parse_readsBinomialBooleanExpression() throws JelException {
        final OperatorExpression exp = parse("$(1).isNumber() && $(a).isString()");

        assertInstanceOf(BooleanExpression.class, exp);
        assertEquals(3, exp.size());
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(0));
        final OperatorSequence o =
            assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertEquals(Operator.AND, o.op);
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(2));
    }

    @Test
    public void parse_readsSimpleRelationalExpression() throws JelException {
        final OperatorExpression exp = parse("$a < $b");

        assertInstanceOf(BooleanExpression.class, exp);
        assertEquals(3, exp.size());
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(0));
        final OperatorSequence o =
            assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertEquals(Operator.LESS_THAN, o.op);
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(2));
    }

    @Test
    public void parse_readsSimpleRelationalExpression_withLeadingComments() throws JelException {
        final OperatorExpression exp = parse("// comment \n $a < $b");

        assertInstanceOf(BooleanExpression.class, exp);
        assertEquals(3, exp.size());
        assertInstanceOf(JelMember.class, exp.spans().get(0));
        final OperatorSequence o =
            assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertEquals(Operator.LESS_THAN, o.op);
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(2));
    }


    @Test
    public void parse_withBinomialArithmeticExpression_readsArithmeticModifier_inSecondClause() throws JelException {
        final OperatorExpression exp = parse("1 + - 4");

        assertInstanceOf(ArithmeticExpression.class, exp);
        assertEquals(4, exp.size());
        assertInstanceOf(LiteralExpression.class, exp.spans().get(0));
        assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertInstanceOf(ModifyingOperatorSequence.class, exp.spans().get(2));
        assertInstanceOf(LiteralExpression.class, exp.spans().get(3));
    }

    @Test
    public void parse_withBinomialArithmeticExpression_andBooleanModifier_throwsException() {
        assertThrows(JelException.class, () -> parse("1 + !4"));
    }

    @Test
    public void parse_withBinomialBooleanExpression_readsBooleanOperator_inSecondClause() throws JelException {
        final OperatorExpression exp = parse("false || !$ref");

        assertInstanceOf(BooleanExpression.class, exp);
        assertEquals(4, exp.size());
        assertInstanceOf(LiteralExpression.class, exp.spans().get(0));
        assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertInstanceOf(ModifyingOperatorSequence.class, exp.spans().get(2));
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(3));
    }

    @Test
    public void parse_withArithmeticExpression_readsAdditionalClauses() throws JelException {
        final OperatorExpression exp = parse("1 + 2 * 3");

        assertInstanceOf(ArithmeticExpression.class, exp);
        assertEquals(5, exp.size());
        assertInstanceOf(LiteralExpression.class, exp.spans().get(0));
        assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertInstanceOf(LiteralExpression.class, exp.spans().get(2));
        assertInstanceOf(OperatorSequence.class, exp.spans().get(3));
        assertInstanceOf(LiteralExpression.class, exp.spans().get(4));
    }

    @Test
    public void parse_withBooleanExpression_readsAdditionalClauses() throws JelException {
        final OperatorExpression exp = parse("1 < 2 && $ref");

        assertInstanceOf(BooleanExpression.class, exp);
        assertEquals(5, exp.size());
        assertInstanceOf(LiteralExpression.class, exp.spans().get(0));
        assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertInstanceOf(LiteralExpression.class, exp.spans().get(2));
        assertInstanceOf(OperatorSequence.class, exp.spans().get(3));
        assertInstanceOf(ReferenceExpression.class, exp.spans().get(4));
    }

    @Test
    public void parse_withMismatchingOperatorTypes_splitsExpression() throws JelException {
        final OperatorExpression exp = parse("1 + 2 < 3 + 4");

        assertInstanceOf(BooleanExpression.class, exp);
        assertEquals(3, exp.size());
        assertInstanceOf(ArithmeticExpression.class, exp.spans().get(0));
        assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertInstanceOf(ArithmeticExpression.class, exp.spans().get(2));
    }

    @Test
    public void parse_toleratesWhitespace_betweenArithmeticOperators() throws JelException {
        final OperatorExpression exp = parse("/*c*/ 1 /*c*/ + /*c*/ 2 /*c*/");

        assertInstanceOf(ArithmeticExpression.class, exp);
        assertEquals(3, exp.size());
        assertInstanceOf(JelMember.class, exp.spans().get(0));
        assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertInstanceOf(JelMember.class, exp.spans().get(2));
    }

    @Test
    public void parse_toleratesWhitespace_betweenBooleanOperators() throws JelException {
        final OperatorExpression exp = parse("1 + 2 /*c*/ || /*c*/ 3");

        assertInstanceOf(BooleanExpression.class, exp);
        assertEquals(3, exp.size());
        assertInstanceOf(ArithmeticExpression.class, exp.spans().get(0));
        assertInstanceOf(OperatorSequence.class, exp.spans().get(1));
        assertInstanceOf(JelMember.class, exp.spans().get(2));
    }

    private static boolean isExpression(final String text) {
        final ContainerToken tokens = Tokenizer.containerize(text);
        return PARSER.isOperatorExpression(tokens.iterator(), tokens.size(), false);
    }

    private static OperatorExpression parse(final String text) throws JelException {
        return PARSER.parse(Tokenizer.containerize(text).iterator());
    }

    private static @Nullable BooleanExpression tryParseDefaultCase(final String text) throws JelException {
        final ContainerToken tokens = Tokenizer.containerize(text);
        return PARSER.tryParseDefaultCase(tokens.iterator(), tokens.size());
    }
}
