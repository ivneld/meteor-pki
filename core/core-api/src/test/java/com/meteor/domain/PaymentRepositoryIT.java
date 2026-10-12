package com.meteor.domain;

import com.meteor.ContextTest;
import com.meteor.enums.PaymentMethod;
import com.meteor.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRepositoryIT extends ContextTest {

    private final PaymentRepository paymentRepository;

    PaymentRepositoryIT(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Test
    void saveExistingAppliesStatusAndTransactionKey() {
        Payment saved = paymentRepository.save(Payment.request(1L, Money.of(10_000), PaymentMethod.CARD));
        saved.approve("tx-1");

        paymentRepository.save(saved);

        Payment found = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(found.getPgTransactionKey()).isEqualTo("tx-1");
        assertThat(found.getAmount()).isEqualTo(Money.of(10_000));
    }

}
