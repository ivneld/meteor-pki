package com.meteor.domain;

import com.meteor.enums.PaymentMethod;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 생성과 상태 변경. 결제할 금액은 주문이 정하므로 usecase 가 {@link OrderReader} 에서 받아 넘긴다. 이 클래스는 주문 저장소를
 * 모른다(R-04).
 */
@Component
@Transactional
public class PaymentWriter {

    private final PaymentRepository paymentRepository;

    public PaymentWriter(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment request(Long orderId, Money amount, PaymentMethod method) {
        return paymentRepository.save(Payment.request(orderId, amount, method));
    }

    public Payment approve(Long paymentId, String pgTransactionKey) {
        Payment payment = load(paymentId);
        payment.approve(pgTransactionKey);
        return paymentRepository.save(payment);
    }

    public Payment fail(Long paymentId) {
        Payment payment = load(paymentId);
        payment.fail();
        return paymentRepository.save(payment);
    }

    private Payment load(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorCode.PAYMENT_NOT_FOUND).property("paymentId", paymentId));
    }

}
