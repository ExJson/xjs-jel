package xjs.jel.serialization.sequence;

import xjs.exception.SyntaxException;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.TokenType;
import xjs.serialization.token.Tokenizer;

import java.util.function.Function;

public class Sequencer {

    public static final Sequencer JEL = new Sequencer(builder());

    protected final ArrayParser arrayParser;
    protected final ConditionalParser conditionalParser;
    protected final DestructureParser destructureParser;
    protected final ElementParser elementParser;
    protected final KeyParser keyParser;
    protected final MatchParser matchParser;
    protected final ModifierParser modifierParser;
    protected final ObjectParser objectParser;
    protected final OperatorParser operatorParser;
    protected final ReferenceParser referenceParser;
    protected final SimpleExpressionParser expressionParser;
    protected final StreamAnalyzer streamAnalyzer;
    protected final ValueParser valueParser;
    protected final WhitespaceCollector whitespaceCollector;

    public Sequencer(final Builder builder) {
        this.arrayParser = builder.arrayParser.apply(this);
        this.conditionalParser = builder.conditionalParser.apply(this);
        this.destructureParser = builder.destructureParser.apply(this);
        this.elementParser = builder.elementParser.apply(this);
        this.keyParser = builder.keyParser.apply(this);
        this.matchParser = builder.matchParser.apply(this);
        this.modifierParser = builder.modifierParser.apply(this);
        this.objectParser = builder.objectParser.apply(this);
        this.operatorParser = builder.operatorParser.apply(this);
        this.referenceParser = builder.referenceParser.apply(this);
        this.expressionParser = builder.expressionParser.apply(this);
        this.streamAnalyzer = builder.streamAnalyzer.apply(this);
        this.valueParser = builder.valueParser.apply(this);
        this.whitespaceCollector = builder.whitespaceCollector.apply(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sequence<?> parse(final String text) throws JelException {
        try {
            return this.parse(Tokenizer.containerize(text));
        } catch (final SyntaxException e) {
            throw JelException.fromSyntaxException(text, e)
                .withDetails("Error occurred when reading tokens");
        }
    }

    public Sequence<?> parse(
            final ContainerToken tokens) throws JelException {
        if (this.isOpenRoot(tokens)) {
            return this.readOpenRoot(tokens);
        }
        return this.elementParser.parse(tokens.iterator());
    }

    protected Sequence<?> readOpenRoot(final ContainerToken tokens) throws JelException {
        // todo: correctly handle header comments
        return this.objectParser.parse(tokens);
    }

    @SuppressWarnings("UnstableApiUsage")
    protected boolean isOpenRoot(final ContainerToken tokens) {
        return tokens.type() == TokenType.OPEN
            && tokens.lookup(':', false) != null;
    }

    public static class Builder {
        private Function<Sequencer, ArrayParser> arrayParser = ArrayParser::new;
        private Function<Sequencer, ConditionalParser> conditionalParser = ConditionalParser::new;
        private Function<Sequencer, DestructureParser> destructureParser = DestructureParser::new;
        private Function<Sequencer, ElementParser> elementParser = ElementParser::new;
        private Function<Sequencer, KeyParser> keyParser = KeyParser::new;
        private Function<Sequencer, MatchParser> matchParser = MatchParser::new;
        private Function<Sequencer, ModifierParser> modifierParser = ModifierParser::new;
        private Function<Sequencer, ObjectParser> objectParser = ObjectParser::new;
        private Function<Sequencer, OperatorParser> operatorParser = OperatorParser::new;
        private Function<Sequencer, ReferenceParser> referenceParser = ReferenceParser::new;
        private Function<Sequencer, SimpleExpressionParser> expressionParser = SimpleExpressionParser::new;
        private Function<Sequencer, StreamAnalyzer> streamAnalyzer = StreamAnalyzer::new;
        private Function<Sequencer, ValueParser> valueParser = ValueParser::new;
        private Function<Sequencer, WhitespaceCollector> whitespaceCollector = WhitespaceCollector::new;

        private Builder() {}

        public Builder arrayParser(final Function<Sequencer, ArrayParser> arrayParser) {
            this.arrayParser = arrayParser;
            return this;
        }

        public Builder conditionalParser(final Function<Sequencer, ConditionalParser> conditionalParser) {
            this.conditionalParser = conditionalParser;
            return this;
        }

        public Builder destructureParser(final Function<Sequencer, DestructureParser> destructureParser) {
            this.destructureParser = destructureParser;
            return this;
        }

        public Builder elementParser(final Function<Sequencer, ElementParser> elementParser) {
            this.elementParser = elementParser;
            return this;
        }

        public Builder keyParser(final Function<Sequencer, KeyParser> keyParser) {
            this.keyParser = keyParser;
            return this;
        }

        public Builder matchParser(final Function<Sequencer, MatchParser> matchParser) {
            this.matchParser = matchParser;
            return this;
        }

        public Builder modifierParser(final Function<Sequencer, ModifierParser> modifierParser) {
            this.modifierParser = modifierParser;
            return this;
        }

        public Builder objectParser(final Function<Sequencer, ObjectParser> objectParser) {
            this.objectParser = objectParser;
            return this;
        }

        public Builder operatorParser(final Function<Sequencer, OperatorParser> operatorParser) {
            this.operatorParser = operatorParser;
            return this;
        }

        public Builder expressionParser(final Function<Sequencer, SimpleExpressionParser> expressionParser) {
            this.expressionParser = expressionParser;
            return this;
        }

        public Builder referenceParser(final Function<Sequencer, ReferenceParser> referenceParser) {
            this.referenceParser = referenceParser;
            return this;
        }

        public Builder streamAnalyzer(final Function<Sequencer, StreamAnalyzer> streamAnalyzer) {
            this.streamAnalyzer = streamAnalyzer;
            return this;
        }

        public Builder valueParser(final Function<Sequencer, ValueParser> valueParser) {
            this.valueParser = valueParser;
            return this;
        }

        public Builder whitespaceCollector(final Function<Sequencer, WhitespaceCollector> whitespaceCollector) {
            this.whitespaceCollector = whitespaceCollector;
            return this;
        }
    }
}
