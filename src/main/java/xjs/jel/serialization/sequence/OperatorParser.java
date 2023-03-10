package xjs.jel.serialization.sequence;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonType;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.ArithmeticExpression;
import xjs.jel.expression.BooleanExpression;
import xjs.jel.expression.DefaultCaseExpression;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.expression.OperatorExpression;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.ModifyingOperator;
import xjs.jel.sequence.ModifyingOperatorSequence;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.jel.sequence.OperatorType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.SymbolToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// -------------------------------------------------------------------------
//                          !!!! WARNING !!!!!!
// -------------------------------------------------------------------------
//
//                 The Eldritch horror that lies below...
//                   is the result of much frustration
//
// -------------------------------------------------------------------------
//
//                        There be dragons here
//
// -------------------------------------------------------------------------
//
//                        Enter at your own risk
//
// -------------------------------------------------------------------------

// todo: Potentially cleaner algorithm:
//   boolSpans = []
//   boolOP = null
//   while (boolOp = nextBoolOp(idx, tokens)) != null:
//     boolSpans += mathOrOperator(itr, boolOp.index, true)
//     boolSpans += boolOp
//     itr.skipTo(boolOp.index + boolOp.size)
//   if boolSpans.hasAny:
//     return BooleanOperator(boolSpans)
//   else:
//     return (OperatorExpression) mathOrOperator(itr, e, false)
//   def mathOrOperator(itr, e, canBeSingle):
//     mathSpans = []
//     mathOp = null
//     while (mathOp = nextMathOp(itr, boolOp.index)) != null:
//       mathModifier = nextMathModifier(itr)
//       if mathModifier != null:
//         mathSpans += mathModifier
//       mathSpans += nextValue()
//       matchSpans += mathOp
//       itr.skipTo(mathOp.index + mathOp.size)

public class OperatorParser extends ParserModule {

    public OperatorParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public boolean isOperatorExpression(
            final ContainerToken.Itr itr, final int e, final boolean inlined) {
        final Token peek = itr.peek();
        if (peek == null) {
            return false;
        } else if (peek.type() == TokenType.PARENTHESES) {
            final ContainerToken p = (ContainerToken) peek;
            if (this.isOperatorExpression(p.iterator(), p.size(), inlined)) {
                return true;
            }
        }
        final int first = this.getFirstOperator(itr, e);
        if (first < 0 || first == skipBackwards(itr, e) - 1) {
            return false;
        } // todo: different parsing when inlined?
        return this.shouldInline(itr, first);
    }

    protected int getFirstOperator(
            final ContainerToken.Itr itr, final int e) {
        final int idx = itr.getIndex();
        int peekAmount = this.skipLeadingModifiers(itr, e);
        Token peek = itr.peek(peekAmount);
        while (peek != null && idx + peekAmount <= e) {
            if (this.isOperator(itr, peek, peekAmount)) {
                return idx + peekAmount - 1;
            }
            peek = itr.peek(++peekAmount);
        }
        return -1;
    }

    protected int skipLeadingModifiers(
            final ContainerToken.Itr itr, final int e) {
        final int idx = itr.getIndex();
        Token peek = itr.peek();
        int peekAmount = 1;
        while (peek != null && idx + peekAmount <= e) {
            if (!JelType.isSignificant(peek) || peek.isSymbol('-') || peek.isSymbol('!')) {
                peek = itr.peek(++peekAmount);
            } else {
                break;
            }
        }
        return peekAmount;
    }

    protected boolean isOperator(
            final ContainerToken.Itr itr, final Token t, final int peekAmount) {
        if (!(t instanceof SymbolToken)) {
            return false;
        }
        switch (((SymbolToken) t).symbol) {
            case '+':
            case '-':
            case '*':
            case '/':
            case '%':
            case '^':
            case '&':
            case '|':
            case '>':
            case '<':
                return true;
            case '!':
            case '=':
                return isFollowedBy(t, itr, peekAmount, '=');
            default:
                return false;
        }
    }

    protected boolean shouldInline(
            final ContainerToken.Itr itr, final int first) {
        final int s = nextSignificant(itr);
        final int e = skipBackwards(itr, first);

        final int peekAmount = s - itr.getIndex() + 1;
        final Token peek = itr.peek(peekAmount);
        assert peek != null;
        if (e - s == 1) {
            switch (peek.type()) {
                case STRING:
                case NUMBER:
                case BRACKETS:
                case BRACES:
                case PARENTHESES:
                    return true;
                default:
                    return false;
            }
        }
        if (this.referenceParser().isReference(itr, peek, peekAmount)) {
            return getNextGap(itr, peekAmount, e) == e;
        }
        return false;
    }

    public @Nullable DefaultCaseExpression tryParseDefaultCase(
            final ContainerToken.Itr itr, final int e) {
        final int next = nextSignificant(itr, e);
        if (next == e) {
            return null;
        }
        final ContainerToken parent = (ContainerToken) itr.getParent();
        final Token peek = parent.get(next);
        if (peek.length() > 1) {
            return null;
        }
        if (skipBackwards(itr, e) != next + 1) {
            return null;
        }
        if (!"_".equals(peek.textOf(parent.reference))) {
            return null;
        }
        final JelMember.Builder builder =
            JelMember.unformattedBuilder(JelType.ELEMENT);
        this.whitespaceCollector().append(builder, itr, e);
        builder.expression(LiteralExpression.of(peek, true));
        itr.next();
        this.whitespaceCollector().append(builder, itr, e);
        return new DefaultCaseExpression(Collections.singletonList(builder.build()));
    }

    public OperatorExpression parse(
            final ContainerToken.Itr itr) throws JelException {
        return this.parse(itr, ((ContainerToken) itr.getParent()).size());
    }

    public OperatorExpression parse(
            final ContainerToken.Itr itr, final int e) throws JelException {
        List<Sequence<?>> sequences = null;
        List<Sequence<?>> buffer = new ArrayList<>();
        OperatorSequence op = this.nextOperator(itr, e);
        OperatorType lastType = null;
        while (op != null) {
            final OperatorType type = op.op.type;
            final ModifyingOperatorSequence modOp =
                this.nextModifyingOperator(itr, op.index);
            final Expression operand =
                this.elementParser().parseInline(itr, op.index, true);
            if (type.isBoolean()) {
                if (sequences == null) {
                    sequences = new ArrayList<>();
                }
                if (modOp != null) {
                    if (modOp.op.type == OperatorType.BOOLEAN
                            && lastType != null
                            && (lastType != OperatorType.BOOLEAN || type == OperatorType.RELATIONAL)) {
                        throw new JelException("Illegal modifying operator")
                            .withSpans(op, modOp)
                            .withDetails("Modifying operator must be at the start of a boolean clause");
                    }
                    sequences.add(modOp);
                }
                if (lastType != null && !lastType.isBoolean()) {
                    buffer.add((Sequence<?>) operand);
                    sequences.add(this.buildArithmetic(buffer));
                    buffer = new ArrayList<>();
                } else {
                    sequences.add((Sequence<?>) operand);
                }
                sequences.add(op);
            } else {
                if (modOp != null) {
                    if (modOp.op.type.isBoolean()) {
                        throw new JelException("Modifying operator mismatch")
                            .withSpans(op, modOp)
                            .withDetails("use of boolean modifier ('!') in arithmetic clause");
                    }
                    buffer.add(modOp);
                }
                buffer.add((Sequence<?>) operand);
                buffer.add(op);
            }
            itr.skipTo(op.index + op.size());
            if (nextSignificant(itr) >= e) {
                throw new JelException("Illegal dangling operator")
                    .withSpan(op)
                    .withDetails("Expected following value");
            }
            op = this.nextOperator(itr, e);
            lastType = type;
        }
        final ModifyingOperatorSequence modOp =
            this.nextModifyingOperator(itr, e);
        if (modOp != null) {
            if ((sequences == null || sequences.isEmpty()) && modOp.op.type == OperatorType.BOOLEAN) {
                sequences = buffer;
            }
            if (lastType != null && !lastType.isCompatibleWithModifier(modOp.op.type)) {
                throw new JelException("Modifying operator mismatch")
                    .withSpan(modOp)
                    .withDetails("use of boolean modifier ('!') in arithmetic clause");
            }
            if (sequences != null && !sequences.isEmpty()) {
                sequences.add(modOp);
            } else {
                buffer.add(modOp);
            }
        }
        // caution: if isExpression && we hit this point with no operators -> stack overflow
        final Expression operand =
            this.elementParser().parseInline(itr, e, true);
        if (lastType == OperatorType.ARITHMETIC) {
            buffer.add((Sequence<?>) operand);
            if (sequences != null && !sequences.isEmpty()) {
                sequences.add(this.buildArithmetic(buffer));
                return new BooleanExpression(sequences);
            }
            return this.buildArithmetic(buffer);
        } else if (sequences == null || sequences.isEmpty()) {
            buffer.add((Sequence<?>) operand);
            return this.buildArithmetic(buffer);
        } else {
            sequences.add((Sequence<?>) operand);
            return new BooleanExpression(sequences);
        }
    }

    protected @Nullable OperatorSequence nextOperator(
            final ContainerToken.Itr itr, final int e) {
        final int s = itr.getIndex();
        int peekAmount = this.skipLeadingModifiers(itr, e);
        final int peekStart = s + peekAmount - 1;
        Token peek = itr.peek(peekAmount);
        boolean anyReferences = false; // prevent ambiguity
        int idx;
        while (peek != null && (idx = s + peekAmount - 1) < e) {
            if (this.referenceParser().isReference(itr, peek, peekAmount)) {
                peek = itr.peek(peekAmount = peekAmount + 2);
                anyReferences = true;
                continue;
            }
            if (idx != peekStart && !anyReferences && peek.type() == TokenType.PARENTHESES) {
                return new OperatorSequence(
                    Operator.MULTIPLY, idx, peek.start(), peek.line(), peek.offset());
            }
            // first symbol after ((-|!)<element>) must be an operator or ()
            if (peek.type() != TokenType.SYMBOL) {
                peek = itr.peek(++peekAmount);
                continue;
            }
            final Operator single =
                this.singleOperator((SymbolToken) peek);
            if (single != null) {
                return new OperatorSequence(single, idx, peek);
            }
            final OperatorSequence multi =
                this.multiOperator((SymbolToken) peek, idx, peekAmount, itr);
            if (multi != null) {
                return multi;
            }
            peek = itr.peek(++peekAmount);
        }
        return null;
    }

    protected @Nullable ModifyingOperatorSequence nextModifyingOperator(
            final ContainerToken.Itr itr, final int e) throws JelException {
        final int idx = itr.getIndex();
        Token peek = itr.peek();
        int peekAmount = 1;
        while (peek != null && idx + peekAmount <= e) {
            if (!JelType.isSignificant(peek)) {
                // todo: capture this whitespace
                peek = itr.peek(++peekAmount);
            } else if (peek.isSymbol('-')) {
                itr.skipTo(idx + peekAmount);
                this.checkModifier(itr);
                return new ModifyingOperatorSequence(ModifyingOperator.INVERT, peek);
            } else if (peek.isSymbol('!')) {
                itr.skipTo(idx + peekAmount);
                this.checkModifier(itr);
                return new ModifyingOperatorSequence(ModifyingOperator.NOT, peek);
            } else {
                break;
            }
        }
        return null;
    }

    protected void checkModifier(final ContainerToken.Itr itr) throws JelException {
        final ContainerToken source = (ContainerToken) itr.getParent();
        final int next = nextSignificant(itr);
        if (next >= source.size()) {
            throw new JelException("Illegal dangling modifier")
                .withSpan(itr.peek(0))
                .withDetails(
                    "Hint: modifier must proceed a reference, parentheses, or positive number");
        }
        final Token peek = source.get(next);
        if (peek.type() != TokenType.PARENTHESES
                && !peek.isSymbol('$')
                && (peek.type() != TokenType.NUMBER || ((NumberToken) peek).number < 0)) {
            throw new JelException("Illegal modifier")
                .withSpans(itr.peek(0), peek)
                .withDetails(
                    "Hint: modifier must proceed a reference, parentheses, or positive number\n"
                    + "Note: this restriction is pending feedback and may get removed");
        }
    }

    protected @Nullable Operator singleOperator(final SymbolToken t) {
        switch (t.symbol) {
            case '+': return Operator.ADD;
            case '-': return Operator.SUBTRACT;
            case '*': return Operator.MULTIPLY;
            case '/': return Operator.DIVIDE;
            case '%': return Operator.MOD;
            case '^': return Operator.POW;
            default: return null;
        }
    }

    protected @Nullable OperatorSequence multiOperator(
            final SymbolToken t, final int idx, final int peekAmount, final ContainerToken.Itr itr) {
        final Token t2 = itr.peek(peekAmount + 1);
        switch (t.symbol) {
            case '&':
                if (t2 != null && t2.isSymbol('&') && t2.start() == t.end())
                    return new OperatorSequence(Operator.AND, idx, t, t2);
                return new OperatorSequence(Operator.BITWISE_AND, idx, t);
            case '|':
                if (t2 != null && t2.isSymbol('|') && t2.start() == t.end())
                    return new OperatorSequence(Operator.OR, idx, t, t2);
                return new OperatorSequence(Operator.BITWISE_OR, idx, t);
            case '>':
                if (t2 != null && t2.start() == t.end()) {
                    if (t2.isSymbol('>')) {
                        return new OperatorSequence(Operator.RIGHT_SHIFT, idx, t, t2);
                    } else if (t2.isSymbol('=')) {
                        return new OperatorSequence(Operator.GREATER_THAN_EQUAL_TO, idx, t, t2);
                    }
                }
                return new OperatorSequence(Operator.GREATER_THAN, idx, t);
            case '<':
                if (t2 != null && t2.start() == t.end()) {
                    if (t2.isSymbol('<')) {
                        return new OperatorSequence(Operator.LEFT_SHIFT, idx, t, t2);
                    } else if (t2.isSymbol('=')) {
                        return new OperatorSequence(Operator.LESS_THAN_EQUAL_TO, idx, t, t2);
                    }
                }
                return new OperatorSequence(Operator.LESS_THAN, idx, t);
            case '=':
                if (t2 != null && t2.isSymbol('=') && t2.start() == t.end())
                    return new OperatorSequence(Operator.EQUAL_TO, idx, t, t2);
                return null;
            case '!':
                if (t2 != null && t2.isSymbol('=') && t2.start() == t.end())
                    return new OperatorSequence(Operator.NOT_EQUAL_TO, idx, t, t2);
                return null;
        }
        return null;
    }

    public OperatorExpression buildArithmetic(final List<Sequence<?>> sequences) {
        final Expression firstExpression = this.getFirstExpression(sequences);
        if (firstExpression.getStrongType() == JsonType.NUMBER || anyPureMath(sequences)) {
            return new ArithmeticExpression(sequences, true);
        } else if (firstExpression instanceof ReferenceExpression) {
            return new ArithmeticExpression(sequences, false);
        }
        return new OperatorExpression(sequences);
    }

    protected Expression getFirstExpression(final List<Sequence<?>> sequences) {
        for (final Span<?> span : sequences) {
            if (span instanceof Expression) {
                return (Expression) span;
            }
        }
        throw new IllegalStateException("unreachable");
    }

    protected boolean anyPureMath(final List<Sequence<?>> sequences) {
        for (final Span<?> span : sequences) {
            if (span instanceof OperatorSequence
                    && ((OperatorSequence) span).op.isPureMath()) {
                return true;
            }
        }
        return false;
    }
}
