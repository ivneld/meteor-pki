package io.autocrypt.vehicles.pki.x509.api.request;

import jakarta.validation.constraints.NotBlank;

public record EkuCreateRequestV1(@NotBlank String oid, @NotBlank String purpose) {
}
