package com.meteor.usecase;

import com.meteor.domain.Address;
import com.meteor.domain.Money;

public record OrderPlaceCommand(Long memberId, String productName, int quantity, Money unitPrice,
        Address shippingAddress) {
}
