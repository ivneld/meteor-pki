package com.meteor.usecase;

import com.meteor.ContextTest;
import com.meteor.domain.Address;
import com.meteor.domain.Email;
import com.meteor.domain.MemberWriter;
import com.meteor.domain.Money;
import com.meteor.domain.Order;
import com.meteor.domain.OrderLimitPolicy;
import com.meteor.enums.OrderStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 회원 확인(MemberReader)과 주문 생성(OrderWriter)을 usecase 가 한 트랜잭션으로 엮는지 확인한다.
 */
class OrderUseCaseIT extends ContextTest {

    private static final Address ADDRESS = new Address("Seoul", "Teheran-ro 1", "06000");

    private final MemberWriter memberWriter;

    private final OrderUseCase orderUseCase;

    OrderUseCaseIT(MemberWriter memberWriter, OrderUseCase orderUseCase) {
        this.memberWriter = memberWriter;
        this.orderUseCase = orderUseCase;
    }

    @Test
    void activeMemberPlacesAnOrder() {
        Long memberId = memberWriter.register(Email.of("order@example.com"), "kim");

        Order order = orderUseCase.place(new OrderPlaceCommand(memberId, "keyboard", 2, Money.of(50_000), ADDRESS));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.totalAmount()).isEqualTo(Money.of(100_000));
    }

    @Test
    void withdrawnMemberCannotOrder() {
        Long memberId = memberWriter.register(Email.of("gone@example.com"), "kim");
        memberWriter.withdraw(memberId);

        assertThatThrownBy(
                () -> orderUseCase.place(new OrderPlaceCommand(memberId, "mouse", 1, Money.of(10_000), ADDRESS)))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_ACTIVE));
    }

    @Test
    void memberCannotHoldMoreAwaitingPaymentOrdersThanTheLimit() {
        Long memberId = memberWriter.register(Email.of("limit@example.com"), "kim");
        for (int i = 0; i < OrderLimitPolicy.MAX_AWAITING_PAYMENT; i++) {
            orderUseCase.place(new OrderPlaceCommand(memberId, "mouse", 1, Money.of(10_000), ADDRESS));
        }

        assertThatThrownBy(
                () -> orderUseCase.place(new OrderPlaceCommand(memberId, "mouse", 1, Money.of(10_000), ADDRESS)))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_LIMIT_EXCEEDED));
    }

}
