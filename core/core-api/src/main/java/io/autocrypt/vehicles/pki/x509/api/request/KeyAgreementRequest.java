package io.autocrypt.vehicles.pki.x509.api.request;

import io.autocrypt.vehicles.pki.x509.enums.SymmetricKeyWrapAlgorithm;
import jakarta.validation.constraints.NotNull;

public record KeyAgreementRequest(@NotNull SymmetricKeyWrapAlgorithm wrappingAlgorithm, @NotNull byte[] initialValue,
        @NotNull byte[] ephemeralPublicKeySpki, @NotNull byte[] wrappedKey) {
}
