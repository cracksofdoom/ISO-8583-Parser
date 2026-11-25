package net.cracksofdoom.parser.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A stateless decoder for common ASCII ISO 8583 wire layouts. */
public final class IsoMessageParser {
    public record FieldSpec(String name, int length, int maxLength, String type, String encoding,
            String lengthEncoding) {
        public FieldSpec(String name, int length, int maxLength, String type) {
            this(name, length, maxLength, type, "ASCII", "ASCII");
        }

        public FieldSpec {
            encoding = encoding == null ? "ASCII" : encoding.toUpperCase();
            lengthEncoding = lengthEncoding == null ? "ASCII" : lengthEncoding.toUpperCase();
            if (name == null || name.isBlank() || name.length() > 80
                    || !List.of("FIXED", "LLVAR", "LLLVAR").contains(type)
                    || length < 0 || length > 999 || maxLength < 0 || (type.equals("FIXED") && length == 0)
                    || (!type.equals("FIXED") && (maxLength == 0 || maxLength > (type.equals("LLVAR") ? 99 : 999)))
                    || !List.of("ASCII", "BCD", "HEX").contains(encoding)
                    || !List.of("ASCII", "BCD").contains(lengthEncoding)) {
                throw new IllegalArgumentException("Invalid field specification");
            }
        }
    }

    public record DecodedField(int id, String name, String value, int length, int offset, int encodedLength) {
    }

    public record Result(String mti, String bitmap, List<Integer> presentFields,
            Map<Integer, DecodedField> fields, int consumedBytes, int totalBytes) {
    }

    private static final Map<Integer, FieldSpec> STANDARD = specs();
    public static final Set<Integer> SENSITIVE_FIELDS = Set.of(2, 35, 36, 45, 52, 53, 55, 64, 102, 103, 128);
    private final Map<Integer, FieldSpec> definitions;

    public IsoMessageParser(Map<Integer, FieldSpec> overrides) {
        Map<Integer, FieldSpec> merged = new LinkedHashMap<>(STANDARD);
        if (overrides != null) {
            overrides.forEach((id, spec) -> {
                if (id == null || id < 2 || id > 128 || id == 65 || spec == null) {
                    throw new IllegalArgumentException(
                            "Field overrides must have IDs 2–128, excluding bitmap field 65");
                }
                merged.put(id, spec);
            });
        }
        definitions = Map.copyOf(merged);
    }

    public Result parse(String input, String format, int headerBytes, boolean revealSensitive) {
        if (input == null || input.isBlank())
            throw new ParseException("Message is required", 0);
        if (headerBytes < 0 || headerBytes > 64)
            throw new ParseException("headerBytes must be between 0 and 64", 0);
        byte[] data;
        boolean asciiBitmap;
        if ("ASCII_HEX_BITMAP".equals(format)) {
            // Preserve payload spaces: they can be part of fixed text fields.
            data = input.replaceFirst("[\\r\\n]+$", "").getBytes(StandardCharsets.ISO_8859_1);
            if (!StandardCharsets.ISO_8859_1.newEncoder().canEncode(input))
                throw new ParseException("ASCII input contains non Latin-1 characters", 0);
            asciiBitmap = true;
        } else if ("HEX_BINARY_BITMAP".equals(format)) {
            String hex = input.replaceAll("[\\s:-]", "");
            if ((hex.length() & 1) != 0 || !hex.matches("[0-9A-Fa-f]+"))
                throw new ParseException("Hex input must contain complete byte pairs", 0);
            data = new byte[hex.length() / 2];
            for (int i = 0; i < data.length; i++)
                data[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            asciiBitmap = false;
        } else
            throw new ParseException("format must be ASCII_HEX_BITMAP or HEX_BINARY_BITMAP", 0);

        Cursor cursor = new Cursor(data, headerBytes);
        String mti = cursor.readAscii(4, "MTI");
        if (!mti.matches("[0-9]{4}"))
            throw new ParseException("MTI must contain four digits", headerBytes);
        String primary = readBitmap(cursor, asciiBitmap);
        String bitmap = primary;
        if ((Integer.parseInt(primary.substring(0, 1), 16) & 8) != 0)
            bitmap += readBitmap(cursor, asciiBitmap);
        if (bitmap.length() == 32 && (Integer.parseInt(bitmap.substring(16, 17), 16) & 8) != 0)
            throw new ParseException("Tertiary bitmaps are not supported", cursor.position);

        List<Integer> present = new ArrayList<>();
        Map<Integer, DecodedField> fields = new LinkedHashMap<>();
        for (int id = 2; id <= bitmap.length() * 4; id++) {
            if (id == 65 || !bitSet(bitmap, id))
                continue;
            present.add(id);
            FieldSpec spec = definitions.get(id);
            if (spec == null)
                throw new ParseException("No definition for field " + id + "; supply a field override",
                        cursor.position);
            int start = cursor.position;
            int length = spec.length();
            if (!spec.type().equals("FIXED")) {
                int digits = spec.type().equals("LLVAR") ? 2 : 3;
                String prefix = spec.lengthEncoding().equals("BCD")
                        ? bcd(cursor.read((digits + 1) / 2, "field " + id + " length"), digits, false, start)
                        : cursor.readAscii(digits, "field " + id + " length");
                if (!prefix.matches("[0-9]+"))
                    throw new ParseException("Invalid length prefix for field " + id, start);
                length = Integer.parseInt(prefix);
                if (length > spec.maxLength())
                    throw new ParseException("Field " + id + " exceeds maximum length " + spec.maxLength(), start);
            }
            boolean binary = spec.encoding().equals("HEX") || id == 52 || id == 64 || id == 128
                    || (!asciiBitmap && id == 55);
            String value = binary
                    ? java.util.HexFormat.of().withUpperCase().formatHex(cursor.read(length, "field " + id))
                    : spec.encoding().equals("BCD")
                            ? bcd(cursor.read((length + 1) / 2, "field " + id), length, true, cursor.position)
                            : cursor.readAscii(length, "field " + id);
            if (!revealSensitive && SENSITIVE_FIELDS.contains(id)) {
                value = id == 2 && value.length() >= 10
                        ? value.substring(0, 6) + "•".repeat(value.length() - 10) + value.substring(value.length() - 4)
                        : "[REDACTED]";
            }
            fields.put(id, new DecodedField(id, spec.name(), value, length, start, cursor.position - start));
        }
        return new Result(mti, bitmap, present, fields, cursor.position, data.length);
    }

    private static boolean bitSet(String hex, int id) {
        int nibble = Integer.parseInt(hex.substring((id - 1) / 4, (id - 1) / 4 + 1), 16);
        return (nibble & (8 >> ((id - 1) % 4))) != 0;
    }

    private static String readBitmap(Cursor cursor, boolean ascii) {
        if (ascii) {
            int start = cursor.position;
            String hex = cursor.readAscii(16, "bitmap");
            if (!hex.matches("[0-9A-Fa-f]{16}"))
                throw new ParseException("Bitmap must be 16 hexadecimal characters", start);
            return hex.toUpperCase();
        }
        return java.util.HexFormat.of().withUpperCase().formatHex(cursor.read(8, "bitmap"));
    }

    private static String bcd(byte[] bytes, int digits, boolean rightPadded, int offset) {
        String hex = java.util.HexFormat.of().withUpperCase().formatHex(bytes);
        String value = digits % 2 == 0 ? hex : rightPadded ? hex.substring(0, digits) : hex.substring(1);
        if (!value.matches("[0-9]+"))
            throw new ParseException("Invalid BCD digits", offset);
        return value;
    }

    private static final class Cursor {
        final byte[] data;
        int position;

        Cursor(byte[] data, int position) {
            this.data = data;
            this.position = position;
        }

        byte[] read(int size, String label) {
            if (position + size > data.length)
                throw new ParseException("Incomplete " + label + ": need " + size + " bytes, have "
                        + Math.max(0, data.length - position), position);
            byte[] bytes = Arrays.copyOfRange(data, position, position + size);
            position += size;
            return bytes;
        }

        String readAscii(int size, String label) {
            return new String(read(size, label), StandardCharsets.ISO_8859_1);
        }
    }

    public static final class ParseException extends RuntimeException {
        private final int offset;

        ParseException(String message, int offset) {
            super(message);
            this.offset = offset;
        }

        public int getOffset() {
            return offset;
        }
    }

    private static Map<Integer, FieldSpec> specs() {
        Map<Integer, FieldSpec> map = new LinkedHashMap<>();
        fixed(map, 3, 6, "Processing code");
        fixed(map, 4, 12, "Amount, transaction");
        fixed(map, 5, 12, "Amount, settlement");
        fixed(map, 6, 12, "Amount, cardholder billing");
        fixed(map, 7, 10, "Transmission date and time");
        fixed(map, 11, 6, "System trace audit number");
        fixed(map, 12, 6, "Local transaction time");
        fixed(map, 13, 4, "Local transaction date");
        fixed(map, 14, 4, "Expiration date");
        fixed(map, 15, 4, "Settlement date");
        fixed(map, 18, 4, "Merchant type");
        fixed(map, 19, 3, "Acquirer country code");
        fixed(map, 22, 3, "POS entry mode");
        fixed(map, 23, 3, "Card sequence number");
        fixed(map, 24, 3, "Function code / network ID");
        fixed(map, 25, 2, "POS condition code");
        fixed(map, 26, 2, "POS PIN capture code");
        fixed(map, 37, 12, "Retrieval reference number");
        fixed(map, 38, 6, "Authorization ID response");
        fixed(map, 39, 2, "Response code");
        fixed(map, 40, 3, "Service restriction code");
        fixed(map, 41, 8, "Card acceptor terminal ID");
        fixed(map, 42, 15, "Card acceptor ID");
        fixed(map, 43, 40, "Card acceptor name/location");
        fixed(map, 49, 3, "Transaction currency code");
        fixed(map, 50, 3, "Settlement currency code");
        fixed(map, 51, 3, "Billing currency code");
        fixed(map, 52, 8, "PIN data");
        fixed(map, 53, 16, "Security control information");
        fixed(map, 64, 8, "Message authentication code");
        fixed(map, 70, 3, "Network management code");
        fixed(map, 90, 42, "Original data elements");
        fixed(map, 95, 42, "Replacement amounts");
        fixed(map, 128, 8, "Message authentication code");
        variable(map, 2, 19, "Primary account number", "LLVAR");
        variable(map, 32, 11, "Acquiring institution ID", "LLVAR");
        variable(map, 33, 11, "Forwarding institution ID", "LLVAR");
        variable(map, 35, 37, "Track 2 data", "LLVAR");
        variable(map, 36, 104, "Track 3 data", "LLLVAR");
        variable(map, 44, 25, "Additional response data", "LLVAR");
        variable(map, 45, 76, "Track 1 data", "LLVAR");
        variable(map, 48, 999, "Additional data, private", "LLLVAR");
        variable(map, 54, 120, "Additional amounts", "LLLVAR");
        variable(map, 55, 999, "ICC data", "LLLVAR");
        for (int id : new int[] { 56, 57, 58, 59, 60, 61, 62, 63, 120, 121, 122, 123, 124, 125, 126, 127 })
            variable(map, id, 999, "Private / reserved data", "LLLVAR");
        variable(map, 100, 11, "Receiving institution ID", "LLVAR");
        variable(map, 102, 28, "Account ID 1", "LLVAR");
        variable(map, 103, 28, "Account ID 2", "LLVAR");
        return map;
    }

    private static void fixed(Map<Integer, FieldSpec> map, int id, int length, String name) {
        map.put(id, new FieldSpec(name, length, length, "FIXED"));
    }

    private static void variable(Map<Integer, FieldSpec> map, int id, int max, String name, String type) {
        map.put(id, new FieldSpec(name, 0, max, type));
    }
}
