package xjs.jel.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.core.StringType;
import xjs.jel.serialization.util.Rereader;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.SymbolToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenStream;
import xjs.serialization.token.TokenType;
import xjs.serialization.token.Tokenizer;
import xjs.serialization.util.PositionTrackingReader;
import xjs.serialization.util.StringContext;

import java.io.IOException;
import java.util.Collections;

// todo: [BUG] check for arithmetic up front or disable altogether
public class Retokenizer extends Tokenizer {

    protected final CharSequence source;
    protected final Token token;
    protected final String text;
    protected final boolean keyExpression;
    protected final CharStack containers;
    protected int arithmeticParentheses;
    protected Token previous;
    protected boolean pathing;
    protected boolean canBeOperator;

    protected Retokenizer(
            final CharSequence source,
            final StringToken token,
            final boolean keyExpression) {
        super(new Rereader(source, token));
        this.source = source;
        this.token = token;
        this.text = token.parsed();
        this.keyExpression = keyExpression;
        this.containers = new CharStack();
        this.canBeOperator = true;
    }

    public static ContainerToken inspect(
            final CharSequence source, final StringToken token, final StringContext ctx) {
        if (ctx == StringContext.KEY) {
            if (token.parsed().contains(">>")) {
                return buildContainers(source, token, true);
            }
            return new ContainerToken(source.toString(), token.start(), token.end(),
                token.line(), token.lastLine(), token.offset(), TokenType.OPEN, Collections.singletonList(token));
        }
        return buildContainers(source, token, false);
    }

    protected static ContainerToken buildContainers(
            final CharSequence source, final StringToken token, final boolean keyExpression) {
        return containerize(new TokenStream(new Retokenizer(source, token, keyExpression), TokenType.OPEN));
    }

    @Override
    protected @Nullable Token single() throws IOException {
        Token t = this.isInlined()
            ? this.readInlined() : this.readNonInlined();
        if (t == null) {
            return null;
        }
        if (t.type() == TokenType.STRING && this.reader.isEndOfText()) {
            if (t.parsed().equals(this.token.parsed())) {
                return this.token;
            }
        }
        this.updateContext(t);
        return t;
    }

    private boolean isInlined() {
        return this.keyExpression ||
            (this.previous != null && (this.pathing || !this.containers.isEmpty()));
    }

    protected @Nullable Token readInlined() throws IOException {
        final Rereader reader = (Rereader) this.reader;
        reader.skipLineWhitespace();
        if (reader.isEndOfText()) {
            return null;
        }
        final int as = reader.absIndex;
        final int al = reader.absLine;
        final int ac = reader.absColumn;
        Token t = super.single();
        if (t == null) {
            return null;
        }
        final int ae = reader.absIndex;
        final int all = reader.absLine;
        if (t.type() == TokenType.WORD) {
            t = t.intoParsed(this.text);
        }
        this.updateSpan(t, as, ae, al, all, ac);
        return t;
    }

    protected @Nullable Token readNonInlined() throws IOException {
        final Rereader reader = (Rereader) this.reader;
        if (reader.isEndOfText()) {
            return null;
        }
        if (this.peekNumberOrOperator()) {
            reader.skipLineWhitespace();
            if (reader.isEndOfText()) {
                return null;
            }
        }
        final int as = reader.absIndex;
        final int al = reader.absLine;
        final int ac = reader.absColumn;
        final Token t = this.nextNonInlined();
        if (t == null) {
            return null;
        }
        final int ae = reader.absIndex;
        final int all = reader.absLine;
        this.updateSpan(t, as, ae, al, all, ac);
        return t;
    }

    protected boolean peekNumberOrOperator() {
        int i = this.reader.index;
        for (; i < this.text.length(); i++) {
            final char c = this.text.charAt(i);
            if (Character.isWhitespace(c)) {
                break;
            }
        }
        if (this.canBeOperator && i == this.text.length() - 1) {
            return true; // fixes trailing whitespace in non-strings
        }
        for (; i < this.text.length(); i++) {
            final char c = this.text.charAt(i);
            if (this.isOperator(c, i) || Character.isDigit(c)) {
                return true;
            } else if (c == '\n' || !Character.isWhitespace(c)) {
                break;
            }
        }
        return false;
    }

    protected boolean isOperator(final char c, final int idx) {
        return this.canBeOperator &&
            ("+-*/^<>".indexOf(c) >= 0
                || this.isArithmeticParentheses(c, idx));
    }

    protected boolean isArithmeticParentheses(final char c, final int idx) {
        if (c == '(') {
            if (this.numberFollows(idx) && this.text.indexOf(')', idx) > idx) {
                this.arithmeticParentheses++;
                return true;
            }
        } else if (c == ')') {
            if (this.arithmeticParentheses > 0) {
                this.arithmeticParentheses--;
                return true;
            }
        }
        return false;
    }

    protected boolean numberFollows(final int idx) {
        for (int i = idx + 1; i < this.text.length(); i++) {
            final char c = this.text.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c == '-' || Character.isDigit(c);
            }
        }
        return false;
    }

    protected @Nullable Token nextNonInlined() throws IOException {
        final PositionTrackingReader reader = this.reader;
        char c = (char) reader.current;
        int s = reader.index;
        int l = reader.line;
        int o = reader.column;

        if (c == '\n') {
            reader.read();
            return new SymbolToken(s, s + 1, l, o, TokenType.BREAK, '\n');
        } else if (c == '.') {
            return this.dot(s, l, o);
        } else if (Character.isDigit(c)) {
            return this.number(s, l, o);
        }
        return this.stringPart(s, l, o);
    }

    protected void updateContext(final Token t) {
        this.updateContainers(t);
        this.updatePathing(t);
        this.previous = t;
    }

    protected void updateContainers(final Token t) {
        if (!this.isInlined()) {
            return;
        }
        if (t instanceof SymbolToken) {
            final char c = ((SymbolToken) t).symbol;
            switch (c) {
                case '{':
                case '[':
                case '(':
                    this.containers.push(c);
                    break;
                case '}':
                    this.expectEndOf('{');
                    break;
                case ']':
                    this.expectEndOf('[');
                    break;
                case ')':
                    this.expectEndOf('(');
            }
        }
    }

    protected void updatePathing(final Token t) {
        final int c = this.reader.current;
        if (t instanceof ParsedToken && c != '.' && c != '(' && c != '[' && c != '{') {
            this.pathing = false;
        } else if (this.containers.isEmpty() &&
                (Character.isWhitespace(c) || c == '\'' || c == '"' || c == ')' || c == ']' || c == '}')) {
            this.pathing = false;
        } else if (t.isSymbol('$')) {
            if (Character.isLetter(c) || c == '.' || c == '(' || c == '[' || c == '{') {
                this.pathing = true;
            }
        }
    }

    protected void expectEndOf(final char opener) {
        if (this.containers.pop() != opener) {
            throw this.reader.expected(opener);
        }
    }

    protected Token stringPart(int i, int l, int o) throws IOException {
        int idx = reader.index;
        do {
            final char c = (char) reader.current; // c != '\n' &&

            if (this.isEscapedPath(c)
                    || (!this.isPathStart(c) && !this.isOperator(c, idx))) {
                this.canBeOperator = false;
                reader.read();
                idx++;
            } else if (idx - i == 0) {
                reader.read();
                return new SymbolToken(i, idx, l, o, c);
            } else {
                break;
            }
        } while (!reader.isEndOfText());
        final String text = this.text.substring(i, reader.index);
        return new StringToken(i, reader.index, l, o, StringType.IMPLICIT, text);
    }

    protected boolean isEscapedPath(final char c) throws IOException {
        if (c != '\\' || reader.index + 1 >= this.text.length()) {
            return false;
        }
        final char peek = this.text.charAt(reader.index + 1);
        if (peek == '$') {
            reader.read();
            return true;
        }
        return false;
    }

    protected boolean isPathStart(final char c) {
        if (c != '$') {
            return false;
        }
        final int i = this.reader.index + 1;
        if (i >= this.text.length()) {
            return false;
        }
        final char peek = this.text.charAt(i);
        return Character.isLetter(peek) || "({[.".indexOf(peek) >= 0;
    }

    protected static class CharStack {
        private char[] stack = new char[10];
        protected int index = 0;

        protected boolean isEmpty() {
            return this.index == 0;
        }

        protected void push(final char c) {
            if (this.index >= this.stack.length) {
                this.grow();
            }
            this.stack[this.index++] = c;
        }

        protected char pop() {
            return this.stack[--this.index];
        }

        protected void grow() {
            final char[] newStack = new char[this.stack.length + 10];
            System.arraycopy(this.stack, 0, newStack, 0, this.index);
            this.stack = newStack;
        }
    }
}
