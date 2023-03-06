package xjs.jel.modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonValue;
import xjs.jel.Alias;
import xjs.jel.JelContext;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ImportModifierTest {
    
    private JelContext ctx;
    
    @BeforeEach
    public void setup() {
        this.ctx = new JelContext(new File(""));
    }

    @Test
    public void modifyMember_setsImportedValue() throws JelException {
        final Modifier modifier =
            new ImportModifier(new ParsedToken(TokenType.WORD, "import"));
        final JelMember member = JelMember.of("value", Json.value("test.xjs"));
        final JsonValue value = Json.value(1234);
        this.ctx.addOutput(new File("/test.xjs"), value);

        modifier.modify(this.ctx, member);
        assertEquals(value, member.getExpression().apply(this.ctx));
    }
    
    @Test
    public void modifyMember_whenAliasIsNull_generatesMembersFromObject() throws JelException {
        final Modifier modifier =
            new ImportModifier(new ParsedToken(TokenType.WORD, "import"));
        final JelMember member = JelMember.of((Alias) null, Json.value("test.xjs"));
        final JsonValue value = Json.object().add("a", 1).add("b", 2);
        this.ctx.addOutput(new File("/test.xjs"), value);

        final List<JelMember> members = modifier.modify(this.ctx, member);
        assertEquals(2, members.size());

        final JelMember one = members.get(0);
        assertEquals("a", one.getAlias().key());
        assertEquals(Json.value(1), one.getExpression().apply(this.ctx));

        final JelMember two = members.get(1);
        assertEquals("b", two.getAlias().key());
        assertEquals(Json.value(2), two.getExpression().apply(this.ctx));
    }

    @Test
    public void modifyMember_whenAliasIsNull_doesNotTolerate_nonObject() {
        final Modifier modifier =
            new ImportModifier(new ParsedToken(TokenType.WORD, "import"));
        final JelMember member = JelMember.of((Alias) null, Json.value("test.xjs"));
        final JsonValue value = Json.value(1234);
        this.ctx.addOutput(new File("/test.xjs"), value);

        assertThrows(JelException.class, () -> modifier.modify(this.ctx, member));
    }
}
