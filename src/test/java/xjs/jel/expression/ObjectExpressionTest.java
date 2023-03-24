package xjs.jel.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.comments.Comment;
import xjs.comments.CommentData;
import xjs.comments.CommentStyle;
import xjs.comments.CommentType;
import xjs.core.Json;
import xjs.core.JsonObject;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.serialization.Span;
import xjs.serialization.token.CommentToken;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.SymbolToken;
import xjs.serialization.token.TokenType;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ObjectExpressionTest {

    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
    }

    @Test
    public void apply_generatesObjectFromMembers() throws JelException {
        final List<Span<?>> members = List.of(
            JelMember.of("a", Json.value(1)),
            JelMember.of("b", Json.value(2)),
            JelMember.of("c", Json.value(3)));
        final ObjectExpression exp = exp(members);
        final JsonObject expected = Json.object()
            .add("a", 1).add("b", 2).add("c", 3);

        assertEquals(expected, exp.apply(this.ctx));
    }

    @Test
    public void apply_appendsLineBreaksAsTrailing() throws JelException {
        final List<Span<?>> members = List.of(
            JelMember.of("a", Json.value(1)),
            new SymbolToken(0, 0, 0, 0, TokenType.BREAK, '\n'),
            new SymbolToken(0, 0, 0, 0, TokenType.BREAK, '\n'));
        final ObjectExpression exp = exp(members);

        assertEquals(2, exp.apply(this.ctx).asObject().getLinesTrailing());
    }

    @Test
    public void apply_appendsCommentsAsInterior() throws JelException {
        final List<Span<?>> members = List.of(
            JelMember.of("a", Json.value(1)),
            new SymbolToken(0, 0, 0, 0, TokenType.BREAK, '\n'),
            new CommentToken(0, 0, 0, 0, CommentStyle.LINE, "a"),
            new SymbolToken(0, 0, 0, 0, TokenType.BREAK, '\n'),
            new CommentToken(0, 0, 0, 0, CommentStyle.HASH, "b"));

        final ObjectExpression exp = exp(members);
        final JsonObject object = exp.apply(this.ctx).asObject();
        final CommentData comments = object.getComments().getData(CommentType.INTERIOR);

        final CommentData expected = new CommentData();
        expected.append(new Comment(CommentStyle.LINE, "a"));
        expected.append(1);
        expected.append(new Comment(CommentStyle.HASH, "b"));

        assertEquals(expected, comments);
        assertEquals(1, object.getLinesTrailing());
    }

    private static ObjectExpression exp(final List<Span<?>> members) {
        return new ObjectExpression(
            new ContainerToken(TokenType.BRACKETS, List.of()), members);
    }
}
