package io.autocrypt.vehicles.pki.x509.api.request;

import jakarta.validation.constraints.NotBlank;

public record EkuCreateRequest(@NotBlank String oid, @NotBlank String purpose) {
}
