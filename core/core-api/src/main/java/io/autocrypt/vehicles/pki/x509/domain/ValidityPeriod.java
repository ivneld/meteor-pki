package io.autocrypt.vehicles.pki.x509.domain;

import java.time.Instant;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

/**
 * 인증서 한 장의 유효기간. notBefore 는 notAfter 보다 반드시 앞선다.
 */
public record ValidityPeriod(Instant notBefore, Instant notAfter) {

    public ValidityPeriod {
        if (notBefore == null || notAfter == null || !notBefore.isBefore(notAfter)) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "ValidityPeriod invalid: notBefore must be strictly before notAfter");
        }
    }

}
