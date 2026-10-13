package com.meteor.pki.domain;

import java.time.Instant;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

public record ValidityPeriod(Instant notBefore, Instant notAfter) {

    public ValidityPeriod {
        if (notBefore == null || notAfter == null || !notBefore.isBefore(notAfter)) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "ValidityPeriod invalid: notBefore must be strictly before notAfter");
        }
    }

}
