package com.meteor.api.request;

import com.meteor.enums.PaymentMethod;
import com.meteor.usecase.PaymentPayCommand;
import jakarta.validation.constraints.NotNull;

public record PaymentPayRequest(@NotNull Long orderId, @NotNull PaymentMethod method) {

    public PaymentPayCommand toCommand() {
        return new PaymentPayCommand(orderId, method);
    }

}
