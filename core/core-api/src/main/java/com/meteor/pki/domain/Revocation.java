package com.meteor.pki.domain;

import java.time.Instant;

import com.meteor.pki.enums.CrlReason;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

public record Revocation(CrlReason reason, Instant revokedAt) {

    public Revocation {
        if (reason == null || revokedAt == null) {
            throw new IllegalArgumentException("Revocation reason and time must not be null.");
        }
        if (reason == CrlReason.REMOVE_FROM_CRL) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "removeFromCRL cannot be used as a revocation reason.");
        }
    }

    public static CrlReason reasonOf(int code) {
        return CrlReason.fromCode(code)
            .orElseThrow(() -> new CoreException(ErrorCode.CERT_INVALID_INPUT, "Unsupported CRL reason code.")
                .property("reason", code));
    }

}
