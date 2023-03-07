package xjs.jel.expression;

import xjs.core.Json;
import xjs.core.JsonArray;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.modifier.Modifier;
import xjs.jel.scope.Scope;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;

import java.util.List;

public class ArrayGeneratorExpression extends Sequence.Combined implements Expression {
    private static final String INDEX_NAME = "i";
    private static final String VALUE_NAME = "v";

    private final TupleExpression input;
    private final List<Modifier> captures;
    private final Expression output;

    public ArrayGeneratorExpression(
            final TupleExpression input, final List<Modifier> captures, final Expression output) {
        super(JelType.ARRAY_GENERATOR, getSpans(output));
        this.input = input;
        this.captures = captures;
        this.output = output;
    }

    @Override
    public JsonArray apply(final JelContext ctx) throws JelException {
        final JsonArray array = new JsonArray();
        final Scope scope = ctx.getScope();
        int i = 0;
        for (final JsonReference ref : this.buildSource(ctx).references()) {
            scope.pushFrame();
            scope.add(INDEX_NAME, new JsonReference(Json.value(i++)));
            scope.add(VALUE_NAME, ref);
            final Expression modified = Modifier.modify(this.output, this.captures);
            final JsonValue v = modified.apply(ctx);
            if (!v.isNull()) {
                array.add(v);
            }
            scope.dropFrame();
        }
        return array;
    }

    private JsonArray buildSource(final JelContext ctx) throws JelException {
        final JsonArray source = new JsonArray();
        for (final Expression exp : this.input.expressions) {
            final JsonValue value;
            if (exp instanceof JelMember) {
                value = ((JelMember) exp).getValue(ctx);
            } else {
                value = exp.apply(ctx);
            }
            if (value.isArray() && this.isExpansion(exp)) {
                source.addAll(value.asArray());
            } else {
                source.add(value);
            }
        }
        return source;
    }

    protected boolean isExpansion(final Expression exp) {
        if (exp instanceof JelMember) {
            return this.isExpansion(((JelMember) exp).getExpression());
        } else if (exp instanceof Sequence<?>) {
            return ((Sequence<?>) exp).type() == JelType.REFERENCE_EXPANSION;
        }
        return false;
    }
}
