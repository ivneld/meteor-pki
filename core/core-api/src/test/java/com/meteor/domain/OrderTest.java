package com.meteor.domain;

import com.meteor.enums.OrderStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Address ADDRESS = new Address("Seoul", "Teheran-ro 1", "06000");

    @Test
    void totalIsUnitPriceTimesQuantity() {
        Order order = Order.place(1L, "keyboard", 2, Money.of(50_000), ADDRESS);

        assertThat(order.totalAmount()).isEqualTo(Money.of(100_000));
        assertThat(order.payableAmount()).isEqualTo(Money.of(100_000));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void quantityMustBePositive() {
        assertThatThrownBy(() -> Order.place(1L, "keyboard", 0, Money.of(1), ADDRESS))
            .isInstanceOf(CoreException.class);
    }

    @Test
    void paidOrderCannotBePaidOrCancelled() {
        Order order = Order.place(1L, "keyboard", 1, Money.of(1), ADDRESS);
        order.markPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThatThrownBy(order::payableAmount).isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_PAYABLE));
        assertThatThrownBy(order::cancel).isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_CANCELLABLE));
    }

}
