package net.cracksofdoom.parser.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

import net.cracksofdoom.parser.core.IsoMessageParser;

class IsoMessageParserTest {
    private final IsoMessageParser parser = new IsoMessageParser(null);
    private static final String ECHO = "0800822000000000000004000000000000001125210025123456301";

    @Test
    void parsesSecondaryBitmapAndFields() {
        var result = parser.parse(ECHO, "ASCII_HEX_BITMAP", 0, false);
        assertEquals("0800", result.mti());
        assertEquals(java.util.List.of(7, 11, 70), result.presentFields());
        assertEquals("1125210025", result.fields().get(7).value());
        assertEquals("301", result.fields().get(70).value());
        assertEquals(result.totalBytes(), result.consumedBytes());
    }

    @Test
    void parsesBinaryBitmapHexAndVariableField() {
        // Field 2 only: 16-digit PAN with an ASCII length prefix.
        byte[] bytes = "0200".getBytes(StandardCharsets.US_ASCII);
        String hex = java.util.HexFormat.of().formatHex(bytes) + "4000000000000000" +
                java.util.HexFormat.of().formatHex("161234567890123456".getBytes(StandardCharsets.US_ASCII));
        var result = parser.parse(hex, "HEX_BINARY_BITMAP", 0, false);
        assertEquals("123456••••••3456", result.fields().get(2).value());
        assertEquals(result.totalBytes(), result.consumedBytes());
    }

    @Test
    void parsesPackedDecimalWithBcdLength() {
        var custom = new IsoMessageParser(
                Map.of(2, new IsoMessageParser.FieldSpec("PAN", 0, 19, "LLVAR", "BCD", "BCD")));
        String hex = "30323030" + "4000000000000000" + "05" + "12345F";
        var result = custom.parse(hex, "HEX_BINARY_BITMAP", 0, true);
        assertEquals("12345", result.fields().get(2).value());
        assertEquals(result.totalBytes(), result.consumedBytes());
    }

    @Test
    void handlesOverridesAndMalformedFrames() {
        var custom = new IsoMessageParser(Map.of(39, new IsoMessageParser.FieldSpec("Response", 3, 3, "FIXED")));
        assertEquals("000",
                custom.parse("02100000000002000000000", "ASCII_HEX_BITMAP", 0, true).fields().get(39).value());
        var ex = assertThrows(IsoMessageParser.ParseException.class,
                () -> parser.parse("0800822000000000000004000000000000001125", "ASCII_HEX_BITMAP", 0, false));
        assertTrue(ex.getMessage().contains("Incomplete field 7"));
        assertThrows(IsoMessageParser.ParseException.class,
                () -> parser.parse("0800ZZZZ000000000000", "ASCII_HEX_BITMAP", 0, false));
    }
}
