package com.meteor.domain;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLimitPolicyTest {

    @Test
    void allowsUpToTheLimit() {
        assertThatCode(() -> OrderLimitPolicy.ensureCanPlace(1L, OrderLimitPolicy.MAX_AWAITING_PAYMENT - 1))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsWhenAwaitingPaymentOrdersReachTheLimit() {
        assertThatThrownBy(() -> OrderLimitPolicy.ensureCanPlace(1L, OrderLimitPolicy.MAX_AWAITING_PAYMENT))
            .isInstanceOf(CoreException.class)
            .extracting(e -> ((CoreException) e).getErrorCode())
            .isEqualTo(ErrorCode.ORDER_LIMIT_EXCEEDED);
    }

}
