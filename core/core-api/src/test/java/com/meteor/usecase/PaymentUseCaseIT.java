package com.meteor.usecase;

import com.meteor.ContextTest;
import com.meteor.domain.Address;
import com.meteor.domain.Email;
import com.meteor.domain.MemberWriter;
import com.meteor.domain.Money;
import com.meteor.domain.OrderReader;
import com.meteor.domain.Payment;
import com.meteor.enums.OrderStatus;
import com.meteor.enums.PaymentMethod;
import com.meteor.enums.PaymentStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 결제가 끝까지 흐르는지 확인한다. local 프로필의 PG 클라이언트는 실제 호출 없이 승인한다. PG 실패와 트랜잭션 분리는
 * {@link PaymentTransactionBoundaryIT} 가 본다.
 */
class PaymentUseCaseIT extends ContextTest {

    private final MemberWriter memberWriter;

    private final OrderUseCase orderUseCase;

    private final OrderReader orderReader;

    private final PaymentUseCase paymentUseCase;

    PaymentUseCaseIT(MemberWriter memberWriter, OrderUseCase orderUseCase, OrderReader orderReader,
            PaymentUseCase paymentUseCase) {
        this.memberWriter = memberWriter;
        this.orderUseCase = orderUseCase;
        this.orderReader = orderReader;
        this.paymentUseCase = paymentUseCase;
    }

    @Test
    void paymentApprovesAndMarksOrderPaid() {
        Long orderId = placeOrder("pay@example.com");

        Payment payment = paymentUseCase.pay(new PaymentPayCommand(orderId, PaymentMethod.CARD));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getAmount()).isEqualTo(Money.of(100_000));
        assertThat(payment.getPgTransactionKey()).isEqualTo("fake-" + payment.getId());
        assertThat(orderReader.find(orderId).getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void paidOrderCannotBePaidAgain() {
        Long orderId = placeOrder("twice@example.com");
        paymentUseCase.pay(new PaymentPayCommand(orderId, PaymentMethod.CARD));

        assertThatThrownBy(() -> paymentUseCase.pay(new PaymentPayCommand(orderId, PaymentMethod.CARD)))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_PAYABLE));
    }

    private Long placeOrder(String email) {
        Long memberId = memberWriter.register(Email.of(email), "kim");
        return orderUseCase
            .place(new OrderPlaceCommand(memberId, "keyboard", 2, Money.of(50_000),
                    new Address("Seoul", "Teheran-ro 1", "06000")))
            .getId();
    }

}
