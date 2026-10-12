package com.meteor.domain;

/**
 * 금액(원). 값 연산만 가진다. 검증 실패는 IllegalArgumentException 으로 던지고 api 계층이 400 으로 바꾼다.
 */
public record Money(long amount) {

    public static final Money ZERO = new Money(0);

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money times(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative: " + quantity);
        }
        return new Money(this.amount * quantity);
    }

    public boolean isZero() {
        return amount == 0;
    }

}
