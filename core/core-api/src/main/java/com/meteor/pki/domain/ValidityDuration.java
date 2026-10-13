package com.meteor.pki.domain;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import com.meteor.pki.enums.ValidityUnit;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

public record ValidityDuration(ValidityUnit unit, long amount) {

    public ValidityDuration {
        if (unit == null) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid period string.");
        }
        if (amount < 1) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Validity period must be positive.");
        }
    }

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

    public Instant maxNotAfter(Instant notBefore) {
        return notBefore.atZone(ZoneOffset.UTC).plus(toPeriod()).toInstant();
    }

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
