package com.meteor.domain;

import java.util.Optional;

import com.meteor.storage.PaymentEntity;
import com.meteor.storage.PaymentJpaRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

/**
 * 결제 저장소 어댑터. storage 모듈의 JPA 엔티티를 애그리거트로 바꾸고, 그 반대도 한다. 엔티티는 이 클래스 밖으로 나가지 않는다(R-05).
 */
@Repository
public class PaymentRepository {

    private final PaymentJpaRepository jpa;

    PaymentRepository(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Payment> findById(Long id) {
        return jpa.findById(id).map(PaymentRepository::toDomain);
    }

    public Payment save(Payment payment) {
        PaymentEntity entity;
        if (payment.isNew()) {
            entity = new PaymentEntity(payment.getOrderId(), payment.getAmount().amount(), payment.getMethod(),
                    payment.getStatus(), payment.getPgTransactionKey());
        }
        else {
            entity = jpa.findById(payment.getId())
                .orElseThrow(
                        () -> new CoreException(ErrorCode.PAYMENT_NOT_FOUND).property("paymentId", payment.getId()));
            entity.changeStatus(payment.getStatus(), payment.getPgTransactionKey());
        }
        return toDomain(jpa.save(entity));
    }

    private static Payment toDomain(PaymentEntity entity) {
        return Payment.restore(entity.getId(), entity.getOrderId(), Money.of(entity.getAmount()), entity.getMethod(),
                entity.getStatus(), entity.getPgTransactionKey());
    }

}
