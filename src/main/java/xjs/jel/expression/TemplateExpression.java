package xjs.jel.expression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.exception.IllegalJelArgsException;
import xjs.jel.exception.JelException;
import xjs.jel.modifier.Modifier;
import xjs.jel.scope.Scope;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;

import java.util.List;

public class TemplateExpression
        extends Sequence.Parent implements Callable {

    private final Expression template;
    private final List<Modifier> modifiers;
    private final List<String> params;
    private @Nullable Scope capture;

    public TemplateExpression(
            final Expression template,
            final List<Modifier> modifiers,
            final List<String> params) {
        super(JelType.TEMPLATE, getSubs(template));
        this.template = template;
        this.modifiers = modifiers;
        this.params = params;
    }

    @Override
    public Expression call(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        this.checkArgs(args);
        final Scope scope = this.getScope(ctx);
        scope.pushFrame();
        this.putArgsInScope(scope, args);

        final Expression exp = this.getModifiedExpression();
        ctx.pushCapture(scope);

        final Expression out;
        if (exp instanceof Callable) {
            final Callable c = (Callable) exp;
            if (c.capturesScope()) {
                c.setCapture(scope.capture());
            }
            out = c;
        } else {
            out = LiteralExpression.of(exp.apply(ctx));
        }
        ctx.dropCapture();
        scope.dropFrame();
        return out;
    }

    // logic mildly redundant -- capture logic handled by parser
    private Expression getModifiedExpression() {
        Expression exp = this.template;
        Modifier capturing = null;
        for (final Modifier modifier : this.modifiers) {
            if (capturing != null) {
                capturing.captureModifier(modifier);
            } if (modifier.capturesModifiers()) {
                capturing = modifier;
            } else {
                exp = modifier.modify(exp);
            }
        }
        if (capturing != null) {
            return capturing.modify(exp);
        }
        return exp;
    }

    protected void checkArgs(final JsonValue... values) throws JelException {
        final int expected = this.params.size();
        if (values.length != expected) {
            throw new IllegalJelArgsException(
                "expected " +  expected + " arguments");
        }
    }

    protected Scope getScope(final JelContext ctx) {
        return this.capture != null ? this.capture : ctx.getScope();
    }

    protected void putArgsInScope(final Scope scope, final JsonValue... args) {
        for (int i = 0; i < this.params.size(); i++) {
            scope.add(this.params.get(i), new JsonReference(args[i]));
        }
    }

    @Override
    public boolean capturesScope() {
        return true;
    }

    @Override
    public void setCapture(final @NotNull Scope scope) {
        this.capture = scope;
    }
}
