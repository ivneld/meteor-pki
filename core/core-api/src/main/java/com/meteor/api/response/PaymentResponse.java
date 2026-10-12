package com.meteor.api.response;

import com.meteor.domain.Payment;
import com.meteor.enums.PaymentMethod;
import com.meteor.enums.PaymentStatus;

public record PaymentResponse(Long id, Long orderId, long amount, PaymentMethod method, PaymentStatus status) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount().amount(),
                payment.getMethod(), payment.getStatus());
    }

}
