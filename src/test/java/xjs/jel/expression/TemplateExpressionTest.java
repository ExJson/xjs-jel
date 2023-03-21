package xjs.jel.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.JelException;
import xjs.jel.modifier.Modifier;
import xjs.jel.modifier.TemplateModifier;
import xjs.jel.path.KeyComponent;
import xjs.jel.path.PathComponent;
import xjs.jel.scope.Scope;
import xjs.jel.sequence.Operator;
import xjs.jel.sequence.OperatorSequence;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TemplateExpressionTest {

    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
        this.ctx.pushParent(Json.object());
    }

    @Test
    public void simpleTemplate_evaluatesRhs() throws JelException {
        final JsonValue value = Json.value(1234);
        final TemplateExpression exp = exp(literal(value));

        assertSame(value, this.call(exp));
    }

    @Test
    public void template_addsArgumentsIntoScope() throws JelException {
        final TemplateExpression exp = exp(path(key("a")), "a");
        final JsonValue value = Json.value(1234);

        assertTrue(value.matches(this.call(exp, value)));
    }

    @Test
    public void template_withCapturedScope_readsFromCapturedScope() throws JelException {
        final JsonValue value = Json.value(1234);
        final Scope scope = this.ctx.getScope();
        scope.pushFrame();
        scope.add("a", new JsonReference(value));

        final TemplateExpression exp = exp(path(key("a")), "a");
        exp.setCapture(this.ctx.getScope().capture());
        scope.dropFrame();

        assertTrue(value.matches(this.call(exp, value)));
    }

    @Test
    public void template_mayReturn_subsequentTemplates() throws JelException {
        final Modifier modifier = modifier("b");
        final Expression rhs = math(path(key("a")), op(Operator.ADD), path(key("b")));
        final TemplateExpression exp = exp(rhs, List.of(modifier), "a");

        final Expression ret = exp.call(Json.object(), this.ctx, Json.value(10));
        assertInstanceOf(Callable.class, ret);
        final Callable next = (Callable) ret;
        final Expression out = next.call(Json.object(), this.ctx, Json.value(15));

        assertTrue(Json.value(25).matches(out.apply(this.ctx)));
    }

    private JsonValue call(final Callable callable, final JsonValue... args) throws JelException {
        return this.call(Json.object(), callable, args);
    }

    private JsonValue call(
            final JsonValue self, final Callable callable, final JsonValue... args) throws JelException {
        return callable.call(self, this.ctx, args).apply(this.ctx);
    }

    private static TemplateExpression exp(final Expression out, String... params) {
        return exp(out, List.of(), params);
    }

    private static TemplateExpression exp(
            final Expression out, final List<Modifier> modifiers, String... params) {
        return new TemplateExpression(out, modifiers, List.of(params));
    }

    private static TemplateModifier modifier(final String... params) {
        final List<ParsedToken> paramTokens =
            Stream.of(params).map(p -> new ParsedToken(TokenType.WORD, p)).toList();
        return new TemplateModifier(
            new ContainerToken(TokenType.OPEN, List.of()), paramTokens);
    }

    private static LiteralExpression literal(final JsonValue value) {
        return LiteralExpression.of(value);
    }

    private static ArithmeticExpression math(final Sequence<?>... subs) {
        return new ArithmeticExpression(List.of(subs), false);
    }

    private static OperatorSequence op(final Operator op) {
        return new OperatorSequence(op, 0);
    }

    private static ReferenceExpression path(final PathComponent... components) {
        final Span<?> first = components[0];
        final Span<?> last = components[components.length - 1];
        return new ReferenceExpression(first, last, Arrays.asList(components));
    }

    private static KeyComponent key(final String key) {
        return new KeyComponent(new ParsedToken(TokenType.WORD, key));
    }
}
