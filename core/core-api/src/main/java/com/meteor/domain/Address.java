package com.meteor.domain;

/**
 * 배송지.
 */
public record Address(String city, String street, String zipCode) {

    public Address {
        requireText(city, "city");
        requireText(street, "street");
        requireText(zipCode, "zipCode");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

}
