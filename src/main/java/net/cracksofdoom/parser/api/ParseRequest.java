package net.cracksofdoom.parser.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.cracksofdoom.parser.core.IsoMessageParser;

import java.util.Map;

public record ParseRequest(
                @NotBlank @Size(max = 16384) String message,
                String format,
                @Min(0) @Max(64) Integer headerBytes,
                Boolean revealSensitive,
                @Size(max = 189) Map<Integer, IsoMessageParser.FieldSpec> fieldOverrides) {
}
