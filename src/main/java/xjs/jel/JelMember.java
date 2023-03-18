package xjs.jel;

import org.jetbrains.annotations.Nullable;
import xjs.comments.Comment;
import xjs.comments.CommentHolder;
import xjs.comments.CommentType;
import xjs.core.JsonLiteral;
import xjs.core.JsonType;
import xjs.core.JsonValue;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.expression.Expression;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.lang.CallableFacade;
import xjs.jel.modifier.Modifier;
import xjs.jel.modifier.TemplateModifier;
import xjs.jel.sequence.AliasType;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.serialization.Span;
import xjs.serialization.token.CommentToken;
import xjs.serialization.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JelMember
        extends Sequence.Combined implements Expression {
    private final List<Modifier> modifiers;
    private Alias alias;
    private Expression expression;
    private final @Nullable JsonValue formatting;
    private boolean modified;

    private JelMember(
            final JelType type,
            final List<Span<?>> subs,
            final Alias alias,
            final List<Modifier> modifiers,
            final @Nullable JsonValue formatting,
            final Expression expression) {
        super(type, subs);
        this.alias = alias;
        this.modifiers = modifiers;
        this.formatting = formatting;
        this.expression = expression;
    }

    private JelMember(
            final Alias alias,
            final List<Modifier> modifiers,
            final Expression expression) {
        this(JelType.MEMBER, Collections.emptyList(),
            alias, modifiers, JsonLiteral.jsonNull(), expression);
    }

    public static JelMember of(final String name, final JsonValue value) {
        return of(name, LiteralExpression.of(value));
    }

    public static JelMember of(
            final String name, final Expression value) {
        return new JelMember(Alias.of(name), new ArrayList<>(), value);
    }

    public static JelMember of(final Alias alias, final JsonValue value) {
        return of(alias, LiteralExpression.of(value));
    }

    public static JelMember of(final Alias alias, final Expression value) {
        return new JelMember(alias, new ArrayList<>(), value);
    }

    public static JelMember of(
            final Alias alias,
            final List<Modifier> modifiers,
            final JsonValue value) {
        return of(alias, modifiers, LiteralExpression.of(value));
    }

    public static JelMember of(
            final Alias alias,
            final List<Modifier> modifiers,
            final Expression value) {
        return new JelMember(alias, modifiers, value);
    }

    public static Builder builder(final JelType type) {
        return new Builder(type, JsonLiteral.jsonNull().setLinesAbove(0).setLinesBetween(0));
    }

    public static Builder unformattedBuilder(final JelType type) {
        return new Builder(type, null);
    }

    @Override
    public JsonValue apply(final JelContext ctx) throws JelException {
        return this.expression.apply(ctx);
    }

    @Override
    public double applyAsNumber(JelContext ctx) throws JelException {
        return this.expression.applyAsNumber(ctx);
    }

    @Override
    public String applyAsString(JelContext ctx) throws JelException {
        return this.expression.applyAsString(ctx);
    }

    @Override
    public boolean applyAsBoolean(JelContext ctx) throws JelException {
        return this.expression.applyAsBoolean(ctx);
    }

    @Override
    public @Nullable JsonType getStrongType() {
        return this.expression.getStrongType();
    }

    public List<Modifier> getModifiers() {
        return this.modifiers;
    }

    public JelMember addModifier(final Modifier modifier) {
        this.modifiers.add(modifier);
        return this;
    }

    public Alias getAlias() {
        return this.alias;
    }

    public JelMember setAlias(final String alias) {
        return this.setAlias(Alias.of(alias));
    }

    public JelMember setAlias(final Alias alias) {
        this.alias = alias;
        return this;
    }

    public int getFlags() {
        return this.getFormatting().getFlags();
    }

    public boolean hasFlag(final int flag) {
        return this.getFormatting().hasFlag(flag);
    }

    public JelMember addFlags(final int flags) {
        this.getFormatting().addFlag(flags);
        return this;
    }

    public JelMember setFlags(final int flags) {
        this.getFormatting().setFlags(flags);
        return this;
    }

    public JelMember setLinesAbove(final int linesAbove) {
        this.getFormatting().setLinesAbove(linesAbove);
        return this;
    }

    public JelMember setLinesBetween(final int linesBetween) {
        this.getFormatting().setLinesBetween(linesBetween);
        return this;
    }

    public JelMember setComments(final CommentHolder comments) {
        this.getFormatting().setComments(comments);
        return this;
    }

    public Expression getExpression() {
        return this.expression;
    }

    public JelMember setExpression(final Expression value) {
        if (this.isModified()) {
            throw new IllegalStateException("already modified");
        }
        this.expression = value;
        return this;
    }

    public JsonValue getFormatting() {
        return Objects.requireNonNull(this.formatting, "expression was inlined");
    }

    public String getKey() {
        return this.alias.key();
    }

    public JsonValue getValue(final JelContext ctx) throws JelException {
        final JsonValue formatting = this.getFormatting();
        final JsonValue v;
        if (this.expression instanceof Callable) {
            v = new CallableFacade((Callable) this.expression);
        } else {
            v = this.expression.apply(ctx);
        }
        return v.setDefaultMetadata(formatting)
            .addFlag(formatting.getFlags());
    }

    public boolean isModified() {
        return this.modified;
    }

    public List<JelMember> process(final JelContext ctx) throws JelException {
        if (this.modifiers.isEmpty()) {
            return Collections.singletonList(this);
        }
        List<JelMember> processed = null;
        for (final Modifier modifier : this.modifiers) {
            if (processed == null) {
                processed = modifier.modify(ctx, this);
                continue;
            } else if (processed.isEmpty()) {
                return processed;
            }
            final List<JelMember> members = processed;
            processed = new ArrayList<>();
            for (final JelMember member : members) {
                processed.addAll(modifier.modify(ctx, member));
            }
        }
        this.modified = true;
        return processed;
    }

    @Override
    public List<Span<?>> flatten() {
        if (!this.isTemplate()) {
            return super.flatten();
        }
        final List<Span<?>> flat = new ArrayList<>();
        for (final Span<?> sub : this.subs) {
            if (sub instanceof Alias &&
                    ((Alias) sub).aliasType() == AliasType.LITERAL) {
                flat.add(new Sequence.Parent(
                    JelType.CALL, Collections.singletonList((Alias) sub)));
            } else if (sub instanceof Sequence) {
                flat.addAll(((Sequence<?>) sub).flatten());
            } else {
                flat.add(sub);
            }
        }
        return flat;
    }

    public boolean isTemplate() {
        for (final Modifier m : this.modifiers) {
            if (m instanceof TemplateModifier) {
                return true;
            }
        }
        return false;
    }

    public static class Builder {
        private final JelType type;
        private final List<Span<?>> subs;
        private final @Nullable JsonValue formatting;
        private final List<Modifier> modifiers;
        private Position position;
        private Alias alias;
        private Expression expression;

        private Builder(final JelType type, final @Nullable JsonValue formatting) {
            this.type = type;
            this.subs = new ArrayList<>();
            this.formatting = formatting;
            this.modifiers = new ArrayList<>();
            this.position = Position.ABOVE;
        }

        public Builder comment(CommentToken comment) {
            if (this.formatting == null) {
                return this.sub(comment);
            }
            final CommentType type;
            switch (this.position) {
                case ABOVE:
                    type = CommentType.HEADER;
                    break;
                case BETWEEN:
                    type = CommentType.VALUE;
                    break;
                default:
                    type = CommentType.EOL;
            }
            this.formatting.getComments()
                .getOrCreate(type)
                .append(new Comment(comment));
            return this.sub(comment);
        }

        public Builder newline(final Token nl) {
            if (this.formatting == null) {
                return this.sub(nl);
            }
            switch (this.position) {
                case ABOVE:
                    if (this.formatting.hasComment(CommentType.HEADER)) {
                        this.formatting.getComments()
                            .getData(CommentType.HEADER)
                            .append(1);
                    } else {
                        this.formatting.setLinesAbove(
                            this.formatting.getLinesAbove() + 1);
                    }
                    break;
                case BETWEEN:
                    if (this.formatting.hasComment(CommentType.VALUE)) {
                        this.formatting.getComments()
                            .getData(CommentType.VALUE)
                            .append(1);
                    } else {
                        this.formatting.setLinesBetween(
                            this.formatting.getLinesBetween() + 1);
                    }
            }
            return this.sub(nl);
        }

        public Builder alias(final Alias alias) {
            this.alias = alias;
            this.position = Position.BETWEEN;
            return this.sub(alias);
        }

        public Builder modifier(final Modifier modifier) {
            this.modifiers.add(modifier);
            this.position = Position.BETWEEN;
            return this.sub(modifier);
        }

        public Builder expression(final Expression expression) {
            this.expression = expression;
            this.position = Position.AFTER;
            return this.sub(expression);
        }

        private Builder sub(final Object sub) {
            if (sub instanceof Span<?>) {
                this.subs.add((Span<?>) sub);
            }
            return this;
        }

        public Alias alias() {
            return this.alias;
        }

        public List<Modifier> modifiers() {
            return this.modifiers;
        }

        public List<Span<?>> subs() {
            return this.subs;
        }

        public JelMember build() {
            if (this.formatting != null
                    && this.formatting.hasComment(CommentType.HEADER)) {
                this.formatting.getComments()
                    .getData(CommentType.HEADER)
                    .trimLastNewline();
            }
            this.subs.sort(Span::compareTo);
            return new JelMember(this.type, this.subs, this.alias,
                this.modifiers, this.formatting, this.expression);
        }
    }

    private enum Position {
        ABOVE,
        BETWEEN,
        AFTER
    }
}
