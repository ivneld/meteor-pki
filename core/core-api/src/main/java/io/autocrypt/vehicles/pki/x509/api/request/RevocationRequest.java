package io.autocrypt.vehicles.pki.x509.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * reason 은 RFC 5280 CRLReason 값.
 */
public record RevocationRequest(@NotBlank String serialNumberHex, @NotNull Integer reason) {
}
