package xjs.jel.modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.JelFlags;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FlagModifierTest {

    private JelContext ctx;

    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
    }

    @Test
    public void modifyMember_addsFlagToMember() throws JelException {
        final Modifier modifier = new FlagModifier(
            new ParsedToken(TokenType.WORD, "private"), JelFlags.PRIVATE);
        final JelMember member = JelMember.of("key", Json.value("value"));

        modifier.modify(this.ctx, member);

        assertTrue(member.hasFlag(JelFlags.PRIVATE));
    }

    @Test
    public void modifyExpression_addsFlagToValue() throws JelException {
        final Modifier modifier = new FlagModifier(
            new ParsedToken(TokenType.WORD, "private"), JelFlags.PRIVATE);
        final Expression modified = modifier.modify(Json.value("value"));
        final JsonValue output = modified.apply(this.ctx);

        assertTrue(output.hasFlag(JelFlags.PRIVATE));
    }
}
