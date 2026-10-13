package com.meteor.pki.api.request;

import com.meteor.pki.enums.SymmetricKeyWrapAlgorithm;
import jakarta.validation.constraints.NotNull;

public record KeyAgreementRequest(@NotNull SymmetricKeyWrapAlgorithm wrappingAlgorithm, @NotNull byte[] initialValue,
        @NotNull byte[] ephemeralPublicKeySpki, @NotNull byte[] wrappedKey) {
}
