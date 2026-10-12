package com.meteor.domain;

import com.meteor.enums.PaymentMethod;
import com.meteor.enums.PaymentStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void requestedPaymentCanBeApproved() {
        Payment payment = Payment.restore(1L, 1L, Money.of(1000), PaymentMethod.CARD, PaymentStatus.REQUESTED, null);

        payment.approve("tx-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getPgTransactionKey()).isEqualTo("tx-1");
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> Payment.request(1L, Money.ZERO, PaymentMethod.CARD))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void settledPaymentCannotChangeAgain() {
        Payment payment = Payment.restore(1L, 1L, Money.of(1000), PaymentMethod.CARD, PaymentStatus.FAILED, null);

        assertThatThrownBy(() -> payment.approve("tx-1")).isInstanceOf(CoreException.class)
            .extracting(e -> ((CoreException) e).getErrorCode())
            .isEqualTo(ErrorCode.PAYMENT_NOT_REQUESTED);
    }

}
