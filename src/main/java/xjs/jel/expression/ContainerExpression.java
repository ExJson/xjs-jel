package xjs.jel.expression;

import xjs.comments.Comment;
import xjs.comments.CommentType;
import xjs.core.JsonContainer;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.CommentToken;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenType;

import java.util.List;

public abstract class ContainerExpression<C extends JsonContainer>
        extends Sequence.Combined implements Expression {

    protected ContainerExpression(
            final JelType type, final ContainerToken source, final List<Span<?>> subs) {
        super(type, source, source, subs);
    }

    @Override
    public C apply(final JelContext ctx) throws JelException {
        final C out = this.newContainer();
        ctx.pushParent(out);
        ctx.getScope().pushFrame();
        try {
            return this.buildContainer(out, ctx);
        } finally {
            ctx.dropParent();
            ctx.getScope().dropFrame();
        }
    }

    @SuppressWarnings("unchecked")
    private C buildContainer(final C out, final JelContext ctx) throws JelException {
        boolean comments = false;
        int trailing = 0;
        for (final Span<?> sub : this.subs) {
            if (sub instanceof Token) {
                if (sub.type() == TokenType.BREAK) {
                    if (comments) {
                        this.appendCommentLine(out);
                    } else {
                        trailing++;
                    }
                } else if (sub.type() == TokenType.COMMENT) {
                    this.appendComment(out, (CommentToken) sub);
                    comments = true;
                }
            } else if (sub instanceof JelMember) {
                for (final JelMember member : ((JelMember) sub).process(ctx)) {
                    this.addMember(ctx, out, member);
                }
            }
        }
        return (C) out.setLinesTrailing(trailing);
    }

    protected abstract C newContainer();

    protected void appendCommentLine(final C c) {
        c.getComments().getData(CommentType.INTERIOR).append(1);
    }

    protected void appendComment(final C c, final CommentToken t) {
        c.getComments().getOrCreate(CommentType.INTERIOR).append(new Comment(t));
    }

    protected abstract void addMember(
        final JelContext ctx, final C c, final JelMember m) throws JelException;
}
