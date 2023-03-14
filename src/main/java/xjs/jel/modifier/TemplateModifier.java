package xjs.jel.modifier;

import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.expression.TemplateExpression;
import xjs.jel.sequence.AliasType;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TemplateModifier
        extends Sequence.Primitive implements Modifier {
    private final List<Modifier> captures;
    private final List<String> params;

    public TemplateModifier(
            final ContainerToken source, final List<ParsedToken> params) {
        super(JelType.IDENTIFIERS, source, source, new ArrayList<>(source.viewTokens()));
        this.captures = new ArrayList<>();
        this.params = buildParams(params);
    }

    private static List<String> buildParams(
            final List<ParsedToken> tokens) {
        final List<String> params = new ArrayList<>();
        for (final ParsedToken token : tokens) {
            params.add(token.parsed());
        }
        return params;
    }

    @Override
    public List<JelMember> modify(
            final JelContext ctx, final JelMember member) throws JelException {
        if (!member.isModified()) {
            final TemplateExpression template = this.modify(member.getExpression());
            template.setCapture(ctx.getScope().capture());
            member.setExpression(this.modify(member.getExpression()));
        }
        return Collections.singletonList(member);
    }

    @Override
    public TemplateExpression modify(final Expression expression) {
        return new TemplateExpression(expression, this.captures, this.params);
    }

    @Override
    public AliasType getAliasType() {
        return AliasType.LITERAL;
    }

    @Override
    public boolean capturesModifiers() {
        return true;
    }

    @Override
    public void captureModifier(final Modifier modifier) {
        this.captures.add(modifier);
    }

    @Override
    public boolean canBeCaptured(final Modifier by) {
        return by instanceof TemplateModifier;
    }

    @Override
    public List<Span<?>> flatten() {
        final List<Span<?>> flat = new ArrayList<>();
        for (final Token sub : this.subs) {
            if (sub instanceof StringToken || sub.type() == TokenType.WORD) {
                flat.add(new Sequence.Primitive(JelType.KEY, Collections.singletonList(sub)));
            } else {
                flat.add(sub);
            }
        }
        return flat;
    }
}
