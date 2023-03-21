package xjs.jel.serialization.sequence;

import org.jetbrains.annotations.Nullable;
import xjs.jel.JelFlags;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.OperatorExpression;
import xjs.jel.expression.ReferenceExpression;
import xjs.jel.modifier.*;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ModifierParser extends ParserModule {

    public ModifierParser(final Sequencer sequencer) {
        super(sequencer);
    }

    public void parse(
            final JelMember.Builder builder, final ContainerToken.Itr itr) throws JelException {
        this.whitespaceCollector().append(builder, itr);
        final List<Modifier> capturing = new ArrayList<>();
        Token peek = itr.peek();
        while (peek != null) {
            if (peek.isSymbol(':')) {
                return;
            } else if (peek.isSymbol('\n')) {
                itr.next();
                peek = itr.peek();
                continue;
            }
            final Modifier modifier = this.next(itr, peek);
            if (modifier == null) {
                throw new JelException("Unknown modifier").withSpan(peek);
            }
            boolean anyCaptured = false;
            for (final Modifier captor : capturing) {
                if (modifier.canBeCaptured(captor)) {
                    captor.captureModifier(modifier);
                    anyCaptured = true;
                    break;
                }
            }
            if (!anyCaptured) {
                builder.modifier(modifier);
            }
            if (modifier.capturesModifiers()) {
                capturing.add(modifier);
            }
            this.whitespaceCollector().append(builder, itr);
            itr.next();
            peek = itr.peek();
        }
    }

    protected @Nullable Modifier next(
            final ContainerToken.Itr itr, final Token current) throws JelException {
        final Modifier delegate = this.readDelegate(itr, current);
        if (delegate != null) {
            return delegate;
        }
        final Modifier defaults = this.readDefaults(itr, current);
        if (defaults != null) {
            return defaults;
        }
        final Modifier template = this.readTemplate(current);
        if (template != null) {
            return template;
        }
        final Modifier conditional = this.readConditional(itr, current);
        if (conditional != null) {
            return conditional;
        }
        final Modifier match = this.readMatch(itr, current);
        if (match != null) {
            return match;
        }
        final Modifier generator = this.readArrayGenerator(current);
        if (generator != null) {
            return generator;
        }
        final Modifier log = this.readLog(itr, current);
        if (log != null) {
            return log;
        }
        return this.readFlag(itr, current);
    }

    protected @Nullable Modifier readDelegate(
            final ContainerToken.Itr itr, final Token current) throws JelException {
        if (!current.isSymbol('@')) {
            return null;
        }
        itr.next();
        final Token peek = itr.peek();
        if (peek == null) {
            throw new JelException("Expected delegate expression")
                .withSpan(current)
                .withDetails("Hint: provide the path to a callable. The rhs will be passed into it");
        }
        final ReferenceExpression ref = this.referenceParser().parse(itr, false);
        if (ref.spans().isEmpty()) {
            throw new JelException("Illegal delegate expression")
                .withSpan(peek)
                .withDetails("Hint: provide the path to a callable. The rhs will be passed into it");
        }
        itr.skip(-1);
        return new DelegateModifier(ref);
    }

    protected @Nullable Modifier readDefaults(
            final ContainerToken.Itr itr, final Token current) throws JelException {
        if (!current.isSymbol('$')) {
            return null;
        }
        final ReferenceExpression exp = this.referenceParser().parse(itr);
        itr.skip(-1); // expecting next()
        return new DefaultsModifier(exp);
    }

    protected @Nullable Modifier readTemplate(final Token current) throws JelException {
        if (current.type() != TokenType.PARENTHESES) {
            return null;
        }
        final ContainerToken tokens = (ContainerToken) current;
        final List<ParsedToken> params =
            this.streamAnalyzer().getKeys(tokens.iterator());
        return new TemplateModifier(tokens, params);
    }

    protected @Nullable Modifier readConditional(
            final ContainerToken.Itr itr, final Token current) throws JelException {
        if (!"if".equals(current.textOf(itr.getReference()))) {
            return null;
        }
        final Token peek = itr.peek(2);
        if (peek == null || peek.type() != TokenType.PARENTHESES) {
            return new ConditionalModifier(current, null);
        }
        final OperatorExpression exp = this.operatorParser()
            .parse(((ContainerToken) peek).iterator());
        itr.next();
        return new ConditionalModifier(current, exp);
    }

    protected @Nullable Modifier readMatch(
            final ContainerToken.Itr itr, final Token current) throws JelException {
        if (!"match".equals(current.textOf(itr.getReference()))) {
            return null;
        }
        itr.next();
        final ReferenceExpression exp = this.referenceParser().parse(itr);
        itr.skip(-1); // expecting next()
        return new MatchModifier(current, exp);
    }

    protected @Nullable Modifier readArrayGenerator(
            final Token current) throws JelException {
        if (current.type() != TokenType.BRACKETS) {
            return null;
        }
        return new ArrayGeneratorModifier(
            this.expressionParser().tupleExpression((ContainerToken) current));
    }

    protected @Nullable Modifier readLog(
            final ContainerToken.Itr itr, final Token current) {
        if (!"log".equals(current.textOf(itr.getReference()))) {
            return null;
        }
        final Token peek = itr.peek(2);
        if (peek != null && "error".equals(peek.textOf(itr.getReference()))) {
            itr.next();
            return new LogModifier(true, current, peek);
        }
        return new LogModifier(false, current);
    }

    protected @Nullable Modifier readFlag(
            final ContainerToken.Itr itr, final Token current) {
        switch (current.textOf(itr.getReference())) {
            case "import": return new ImportModifier(current);
            case "require": return new RequireModifier(current);
            case "add": return new AddModifier(current);
            case "merge": return new MergeModifier(current);
            case "log": return new LogModifier(false, current);
            case "set": return new SetModifier(current);
            case "from": return new DestructureModifier(current);
            case "var": return new FlagModifier(current, JelFlags.VAR);
            case "private": return new FlagModifier(current, JelFlags.PRIVATE | JelFlags.VAR);
            case "noinline": return new NoInlineModifier(current);
            case "raise": return new RaiseModifier(current);
            case "yield": return new YieldModifier(current);
            default: return null;
        }
    }
}
