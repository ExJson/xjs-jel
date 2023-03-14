package xjs.jel.integration;

import org.junit.jupiter.api.Test;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.LiteralExpression;
import xjs.jel.expression.ObjectExpression;
import xjs.jel.sequence.Sequence;
import xjs.serialization.token.TokenType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public final class KeyParserIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void parse_readsSimpleKeyValuePair() {
        this.inputSuccess("""
            key: value
            """);
        this.outputTrimmed("""
            key: value
            """);
    }

    @Test
    public void parse_withValueOnSubsequentLines_readsMember() {
        this.inputSuccess("""
            key:
            
              value
            """);
        this.outputTrimmed("""
            key:
            
              value
            """);
    }

    @Test
    public void parse_withSeparatorOnSubsequentLines_readsMember() {
        this.inputSuccess("""
            key
              :
              value
            """);
        this.outputTrimmed("""
            key:
            
              value
            """);
    }

    @Test
    public void parse_toleratesVoidString() {
        this.inputSuccess("""
            : value
            """);
        this.outputTrimmed("""
            : value
            """);
    }

    @Test
    public void parse_readsOtherLiteralExpressions_asStrings() throws JelException {
        final Sequence<?> sequence = this.sequence("""
            true: value
            1234: value
            """);
        final ObjectExpression o = assertInstanceOf(ObjectExpression.class, sequence);
        assertEquals(3, o.size());
        final JelMember m1 = assertInstanceOf(JelMember.class, o.spans().get(0));
        assertInstanceOf(LiteralExpression.OfString.class, m1.getAlias().spans().get(0));
        final JelMember m2 = assertInstanceOf(JelMember.class, o.spans().get(1));
        assertInstanceOf(LiteralExpression.OfString.class, m2.getAlias().spans().get(0));
        assertEquals(TokenType.BREAK, o.spans().get(2).type());
    }
}
