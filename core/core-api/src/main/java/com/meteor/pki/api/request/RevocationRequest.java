package com.meteor.pki.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RevocationRequest(@NotBlank String serialNumberHex, @NotNull Integer reason) {
}
