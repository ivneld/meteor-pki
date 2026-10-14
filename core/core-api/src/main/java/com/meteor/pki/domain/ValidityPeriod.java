package com.meteor.pki.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

public record ValidityPeriod(Instant notBefore, Instant notAfter) {

    public ValidityPeriod {
        notBefore = notBefore == null ? null : notBefore.truncatedTo(ChronoUnit.SECONDS);
        notAfter = notAfter == null ? null : notAfter.truncatedTo(ChronoUnit.SECONDS);
        if (notBefore == null || notAfter == null || !notBefore.isBefore(notAfter)) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "ValidityPeriod invalid: notBefore must be strictly before notAfter");
        }
    }

    public boolean contains(ValidityPeriod other) {
        return !other.notBefore.isBefore(notBefore) && !other.notAfter.isAfter(notAfter);
    }

}
