package io.autocrypt.vehicles.pki.x509.domain;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import io.autocrypt.vehicles.pki.x509.enums.ValidityUnit;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

/**
 * 프로파일이 허용하는 인증서 유효기간의 길이(x509-ra 의 ValidityPolicy). 단위 하나(년, 월, 일)와 양의 정수 하나다. 기간 계산은
 * UTC 달력 기준이다.
 */
public record ValidityDuration(ValidityUnit unit, long amount) {

    public ValidityDuration {
        if (unit == null) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid period string.");
        }
        if (amount < 1) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Validity period must be positive.");
        }
    }

    /** ISO-8601 기간 문자열(P1Y, P6M, P90D)을 읽는다. 단위가 둘 이상이면 거절한다. */
    public static ValidityDuration from(String text) {
        Period period;
        try {
            period = Period.parse(text == null ? "" : text);
        }
        catch (DateTimeParseException ex) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid period string.");
        }
        int nonZero = (period.getYears() != 0 ? 1 : 0) + (period.getMonths() != 0 ? 1 : 0)
                + (period.getDays() != 0 ? 1 : 0);
        if (nonZero != 1) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "Period must have exactly one non-zero unit (years, months, or days).");
        }
        if (period.getYears() != 0) {
            return new ValidityDuration(ValidityUnit.YEARS, period.getYears());
        }
        if (period.getMonths() != 0) {
            return new ValidityDuration(ValidityUnit.MONTHS, period.getMonths());
        }
        return new ValidityDuration(ValidityUnit.DAYS, period.getDays());
    }

    public Period toPeriod() {
        return switch (unit) {
            case YEARS -> Period.ofYears(Math.toIntExact(amount));
            case MONTHS -> Period.ofMonths(Math.toIntExact(amount));
            case DAYS -> Period.ofDays(Math.toIntExact(amount));
        };
    }

    public String toPeriodString() {
        return toPeriod().toString();
    }

    /** notBefore 에서 이 정책으로 허용되는 가장 늦은 notAfter. */
    public Instant maxNotAfter(Instant notBefore) {
        return notBefore.atZone(ZoneOffset.UTC).plus(toPeriod()).toInstant();
    }

    /**
     * 요청한 기간을 이 정책 안으로 확정한다. notAfter 가 없으면 정책의 최대 기간을 쓰고, 정책보다 길면 거절한다. 요청은 기간을 줄일 수만
     * 있다.
     */
    public ValidityPeriod resolve(Instant notBefore, Instant requestedNotAfter) {
        Instant max = maxNotAfter(notBefore);
        if (requestedNotAfter == null) {
            return new ValidityPeriod(notBefore, max);
        }
        if (requestedNotAfter.isAfter(max)) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Validity is outside allowed policy range.");
        }
        return new ValidityPeriod(notBefore, requestedNotAfter);
    }

}
