package xjs.jel.destructuring;

import xjs.core.JsonContainer;
import xjs.core.JsonObject;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.ContainerToken;

import java.util.ArrayList;

public abstract class DestructurePattern extends Sequence.Combined {

    protected DestructurePattern(
            final JelType type, final ContainerToken source) {
        super(type, source, source, new ArrayList<>(source.viewTokens()));
    }

    public abstract void destructure(
        final JsonContainer from, final JsonObject into) throws JelException;

    protected JelException error(final String msg, final Object details) {
        return this.error(msg, this, details);
    }

    protected JelException error(
            final String msg, final Span<?> span, final Object details) {
        return new JelException(msg)
            .withSpan(span)
            .withDetails("Cannot destructure: " + details);
    }
}
