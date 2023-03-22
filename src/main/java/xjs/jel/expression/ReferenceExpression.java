package xjs.jel.expression;

import xjs.core.JsonCopy;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelFlags;
import xjs.jel.exception.JelException;
import xjs.jel.path.JsonPath;
import xjs.jel.path.PathComponent;
import xjs.jel.sequence.JelType;
import xjs.serialization.Span;

import java.util.List;

public class ReferenceExpression
        extends JsonPath implements Expression {

    public ReferenceExpression(
            final Span<?> s, final Span<?> e, final List<PathComponent> components) {
        super(JelType.REFERENCE, s, e, components);
    }

    private ReferenceExpression(
            final JelType type, final Span<?> s, final Span<?> e, final List<PathComponent> components) {
        super(type, s, e, components);
    }

    public static ReferenceExpression expansion(
            final Span<?> s, final Span<?> e, final List<PathComponent> components) {
        return new ReferenceExpression(JelType.REFERENCE_EXPANSION, s, e, components);
    }

    @Override
    public JsonValue apply(final JelContext ctx) throws JelException {
        JsonValue value = this.get(ctx);
        if (!value.hasFlag(JelFlags.CREATED)) {
            value = value.copy(JsonCopy.RECURSIVE | JsonCopy.FORMATTING);
        }
        return value.setLinesAbove(-1)
            .setLinesBetween(-1)
            .setFlags(JelFlags.NULL);
    }

    @Override
    public String applyAsString(final JelContext ctx) throws JelException {
        return this.get(ctx).intoString();
    }

    @Override
    public final boolean isInlined() {
        return true;
    }
}
