package net.cracksofdoom.parser.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

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
    void parsesLvarWithProfileOverride() {
        var custom = new IsoMessageParser(Map.of(48, new IsoMessageParser.FieldSpec("Private", 0, 9, "LVAR")));
        var result = custom.parse("020000000000000100005HELLO", "ASCII_HEX_BITMAP", 0, false);
        assertEquals("HELLO", result.fields().get(48).value());
        assertEquals(6, result.fields().get(48).encodedLength());
        assertEquals(result.totalBytes(), result.consumedBytes());
        assertThrows(IllegalArgumentException.class,
                () -> new IsoMessageParser.FieldSpec("Private", 0, 10, "LVAR"));
    }

    @Test
    void parsesTertiaryBitmapWithProfileDefinition() {
        var custom = new IsoMessageParser(Map.of(130, new IsoMessageParser.FieldSpec("Tertiary data", 3, 3, "FIXED")));
        String frame = "0200" + "8000000000000000" + "8000000000000000" + "4000000000000000" + "ABC";
        var result = custom.parse(frame, "ASCII_HEX_BITMAP", 0, false);
        assertEquals(48, result.bitmap().length());
        assertEquals(java.util.List.of(130), result.presentFields());
        assertEquals("ABC", result.fields().get(130).value());
        assertEquals(result.totalBytes(), result.consumedBytes());
        assertThrows(IsoMessageParser.ParseException.class,
                () -> parser.parse(frame, "ASCII_HEX_BITMAP", 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> new IsoMessageParser(Map.of(129, new IsoMessageParser.FieldSpec("Bitmap", 8, 8, "FIXED"))));
    }

    @Test
    void decodesBinaryIccDataConsistentlyAcrossFrameFormats() {
        String asciiFrame = "0200" + "0000000000000200" + "003" + "\u0001\u00A0\u00FF";
        var ascii = parser.parse(asciiFrame, "ASCII_HEX_BITMAP", 0, true);
        assertEquals("01A0FF", ascii.fields().get(55).value());
        String hexFrame = "30323030" + "0000000000000200" + "303033" + "01A0FF";
        var binary = parser.parse(hexFrame, "HEX_BINARY_BITMAP", 0, true);
        assertEquals(ascii.fields().get(55).value(), binary.fields().get(55).value());
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
