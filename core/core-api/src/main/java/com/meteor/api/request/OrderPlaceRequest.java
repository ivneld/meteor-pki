package com.meteor.api.request;

import com.meteor.domain.Address;
import com.meteor.domain.Money;
import com.meteor.usecase.OrderPlaceCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderPlaceRequest(@NotNull Long memberId, @NotBlank String productName, @Min(1) int quantity,
        @Min(0) long unitPrice, @NotNull @Valid AddressRequest shippingAddress) {

    public OrderPlaceCommand toCommand() {
        return new OrderPlaceCommand(memberId, productName, quantity, Money.of(unitPrice), shippingAddress.toAddress());
    }

    public record AddressRequest(@NotBlank String city, @NotBlank String street, @NotBlank String zipCode) {

        Address toAddress() {
            return new Address(city, street, zipCode);
        }

    }

}
