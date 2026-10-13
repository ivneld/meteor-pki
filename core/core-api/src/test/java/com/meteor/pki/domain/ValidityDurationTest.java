package com.meteor.pki.domain;

import java.time.Instant;

import com.meteor.pki.enums.ValidityUnit;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidityDurationTest {

    private static final Instant NOT_BEFORE = Instant.parse("2026-01-31T00:00:00Z");

    @Test
    void readsOneUnitPeriods() {
        assertThat(ValidityDuration.from("P2Y")).isEqualTo(new ValidityDuration(ValidityUnit.YEARS, 2));
        assertThat(ValidityDuration.from("P6M")).isEqualTo(new ValidityDuration(ValidityUnit.MONTHS, 6));
        assertThat(ValidityDuration.from("P90D").toPeriodString()).isEqualTo("P90D");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "1Y", "P1Y2M", "P0D" })
    void rejectsMalformedOrMixedPeriods(String text) {
        assertThatThrownBy(() -> ValidityDuration.from(text)).isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.CERT_INVALID_INPUT));
    }

    @Test
    void calendarArithmeticInUtc() {
        assertThat(ValidityDuration.from("P1M").maxNotAfter(NOT_BEFORE))
            .isEqualTo(Instant.parse("2026-02-28T00:00:00Z"));
    }

    @Test
    void requestCanOnlyShortenThePolicy() {
        ValidityDuration policy = ValidityDuration.from("P1Y");

        assertThat(policy.resolve(NOT_BEFORE, null).notAfter()).isEqualTo(Instant.parse("2027-01-31T00:00:00Z"));
        assertThat(policy.resolve(NOT_BEFORE, Instant.parse("2026-06-01T00:00:00Z")).notAfter())
            .isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThatThrownBy(() -> policy.resolve(NOT_BEFORE, Instant.parse("2027-02-01T00:00:00Z")))
            .hasMessage("Validity is outside allowed policy range.");
        assertThatThrownBy(() -> policy.resolve(NOT_BEFORE, NOT_BEFORE))
            .hasMessage("ValidityPeriod invalid: notBefore must be strictly before notAfter");
    }

}
