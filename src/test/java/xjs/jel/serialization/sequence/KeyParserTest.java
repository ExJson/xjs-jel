package xjs.jel.serialization.sequence;

import org.junit.jupiter.api.Test;
import xjs.jel.JelMember;
import xjs.jel.exception.JelException;
import xjs.jel.modifier.FlagModifier;
import xjs.jel.modifier.LogModifier;
import xjs.jel.sequence.JelType;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.Tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class KeyParserTest {

    private static final KeyParser PARSER = Sequencer.JEL.keyParser;

    @Test
    public void parse_appendsSimpleAlias() throws JelException {
        final JelMember member = parse("this is a key:");

        assertEquals("this is a key", member.getKey());
    }

    @Test
    public void parse_appendsAlias_withSimpleModifier() throws JelException {
        final JelMember member = parse("key >> private:");

        assertEquals("key", member.getKey());
        assertEquals(1, member.getModifiers().size());
        assertInstanceOf(FlagModifier.class, member.getModifiers().get(0));
    }

    @Test
    public void parse_appendsAlias_withMultipleModifiers() throws JelException {
        final JelMember member = parse("key >> private var:");

        assertEquals("key", member.getKey());
        assertEquals(2, member.getModifiers().size());
        assertInstanceOf(FlagModifier.class, member.getModifiers().get(0));
        assertInstanceOf(FlagModifier.class, member.getModifiers().get(1));
    }

    @Test
    public void parse_appendsModifiersOnly() throws JelException {
        final JelMember member = parse(">> log:");

        assertNull(member.getAlias());
        assertEquals(1, member.getModifiers().size());
        assertInstanceOf(LogModifier.class, member.getModifiers().get(0));
    }

    @Test
    public void parse_withNoAliasOrModifiers_isEmptyString() throws JelException {
        parse("template >> (a):");
        final JelMember member = parse(":");

        assertEquals("", member.getKey());
    }

    @Test
    public void parse_whenModifierRequires_specificAlias_parsesAliasType() throws JelException {

    }

    @Test
    public void parse_whenMultipleModifiersRequire_specificAlias_throwsException() throws JelException {

    }

    private static JelMember parse(final String text) throws JelException {
        final JelMember.Builder builder = JelMember.builder(JelType.MEMBER);
        final ContainerToken tokens = Tokenizer.containerize(text);
        PARSER.parse(builder, tokens.iterator());
        return builder.build();
    }
}
