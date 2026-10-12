package com.meteor.domain;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 조회. 읽기 전용 트랜잭션에서 돌고, 호출자에게 트랜잭션이 있으면 거기에 참여한다(R-07).
 */
@Component
@Transactional(readOnly = true)
public class PaymentReader {

    private final PaymentRepository paymentRepository;

    public PaymentReader(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment find(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorCode.PAYMENT_NOT_FOUND).property("paymentId", paymentId));
    }

}
