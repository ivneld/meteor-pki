package com.meteor.usecase;

import com.meteor.enums.PaymentMethod;

public record PaymentPayCommand(Long orderId, PaymentMethod method) {
}
