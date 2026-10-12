package com.meteor.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void acceptsWellFormed() {
        assertThat(Email.of("user@example.com").value()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsMalformed() {
        assertThatThrownBy(() -> Email.of("not-an-email")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Email.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

}
