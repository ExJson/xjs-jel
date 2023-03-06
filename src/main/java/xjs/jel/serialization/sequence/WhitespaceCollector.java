package xjs.jel.serialization.sequence;

import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.serialization.Span;
import xjs.serialization.token.CommentToken;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.TokenStream;
import xjs.serialization.token.TokenType;

import java.util.List;

public class WhitespaceCollector extends ParserModule {

    public WhitespaceCollector(final Sequencer sequencer) {
        super(sequencer);
    }

    public void append(final JelMember.Builder builder, final ContainerToken.Itr itr) {
        append(builder, itr, ((ContainerToken) itr.getParent()).size());
    }

    public void append(
            final JelMember.Builder builder, final ContainerToken.Itr itr, final int e) {
        Token peek = itr.peek();
        while (peek != null) {
            if (itr.getIndex() >= e) {
                break;
            }
            if (peek.type() == TokenType.COMMENT) {
                builder.comment((CommentToken) peek);
            } else if (peek.type() == TokenType.BREAK) {
                builder.newline(peek);
            } else {
                break;
            }
            itr.next();
            peek = itr.peek();
        }
    }

    public void append(
            final List<Span<?>> spans, final ContainerToken.Itr itr) {
        Token peek = itr.peek();
        while (peek != null) {
            if (peek.type() == TokenType.COMMENT
                    || peek.type() == TokenType.BREAK) {
                spans.add(peek);
            } else {
                break;
            }
            itr.next();
            peek = itr.peek();
        }
    }

    public boolean appendLineComments(
            final JelMember.Builder builder, final ContainerToken.Itr itr) {
        return this.appendLineComments(builder, itr, Integer.MAX_VALUE);
    }

    public boolean appendLineComments(
            final JelMember.Builder builder, final ContainerToken.Itr itr, final int e) {
        Token peek = itr.peek();
        int peekAmount = 1;
        boolean commaFound = false;
        while (peek != null) {
            if (itr.getIndex() >= e) {
                break;
            } else if (peek.isSymbol(',')) {
                if (commaFound) {
                    break;
                }
                commaFound = true;
            } else if (peek.type() == TokenType.COMMENT) {
                builder.comment((CommentToken) peek);
            } else {
                break;
            }
            peek = itr.peek(++peekAmount);
        }
        return commaFound;
    }

    // should be temporary -- prevents syntax highlighting
    public void skip(final ContainerToken.Itr itr) {
        Token peek = itr.peek();
        while (peek != null) {
            if (peek.type() == TokenType.COMMENT
                    || peek.type() == TokenType.BREAK) {
                itr.next();
                peek = itr.peek();
            } else {
                break;
            }
        }
    }

    public void delimit(
            final JelMember.Builder builder, final TokenStream.Itr itr) {
        boolean commaFound = false;
        boolean nlFound = false;
        Token peek;
        while ((peek = itr.peek()) != null) {
            if (peek.type() == TokenType.BREAK) {
                builder.newline(peek);
                nlFound = true;
            } else if (peek.type() == TokenType.COMMENT) {
                if (nlFound) { // comment is attached to previous member
                    builder.comment((CommentToken) peek);
                }
            } else if (peek.isSymbol(',')) {
                if (commaFound) {
                    break;
                }
                commaFound = true;
            } else {
                break;
            }
            itr.next();
        }
    }

    public void delimit(final TokenStream.Itr itr) throws JelException {
        if (!this.tryDelimit(itr)) {
            throw new JelException("Expected delimiter")
                .withSpan(itr.peek())
                .withDetails("Hint: elements must be separated by ',' or a newline");
        }
    }

    public boolean tryDelimit(final TokenStream.Itr itr) {
        boolean commaFound = false;
        boolean nlFound = false;
        Token peek;
        while ((peek = itr.peek()) != null) {
            if (peek.type() == TokenType.BREAK
                    || peek.type() == TokenType.COMMENT) {
                nlFound = true;
                itr.next();
            } else if (peek.isSymbol(',')) {
                if (commaFound) {
                    break;
                }
                commaFound = true;
                itr.next();
            } else {
                break;
            }
        }
        return commaFound || nlFound || !itr.hasNext();
    }
}
