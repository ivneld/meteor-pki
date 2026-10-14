package com.meteor.pki.domain;

import java.time.Duration;
import java.time.Instant;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

public record RenewalOptions(int activationStartDaysAgo, ValidityDuration requestedDuration) {

    public RenewalOptions {
        if (activationStartDaysAgo < 0) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "activationStartDaysAgo must not be negative.");
        }
    }

    ValidityPeriod validity(Instant now, ValidityPeriod original) {
        Instant notBefore = now.minus(Duration.ofDays(activationStartDaysAgo));
        Instant notAfter = requestedDuration == null ? notBefore.plus(original.length())
                : requestedDuration.maxNotAfter(notBefore);
        return new ValidityPeriod(notBefore, notAfter);
    }

}
