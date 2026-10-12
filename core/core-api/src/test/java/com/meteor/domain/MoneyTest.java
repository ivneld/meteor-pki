package com.meteor.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void arithmetic() {
        assertThat(Money.of(1000).times(3).plus(Money.of(500))).isEqualTo(Money.of(3500));
        assertThat(Money.ZERO.isZero()).isTrue();
    }

    @Test
    void rejectsNegative() {
        assertThatThrownBy(() -> Money.of(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of(1).times(-1)).isInstanceOf(IllegalArgumentException.class);
    }

}
