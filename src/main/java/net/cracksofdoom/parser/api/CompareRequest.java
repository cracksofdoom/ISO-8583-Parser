package net.cracksofdoom.parser.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CompareRequest(@Valid @NotNull ParseRequest left, @Valid @NotNull ParseRequest right) {
}
