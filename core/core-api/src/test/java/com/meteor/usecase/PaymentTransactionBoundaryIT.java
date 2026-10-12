package com.meteor.usecase;

import java.util.List;

import com.meteor.ContextTest;
import com.meteor.client.pg.PgApproval;
import com.meteor.client.pg.PgApprovalException;
import com.meteor.client.pg.PgClient;
import com.meteor.domain.Address;
import com.meteor.domain.Email;
import com.meteor.domain.MemberWriter;
import com.meteor.domain.Money;
import com.meteor.domain.OrderReader;
import com.meteor.domain.OrderWriter;
import com.meteor.enums.OrderStatus;
import com.meteor.enums.PaymentMethod;
import com.meteor.enums.PaymentStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * PaymentUseCase 가 나눈 트랜잭션 경계를 확인한다.
 *
 * <pre>
 * 트랜잭션 1 (결제 REQUESTED) → PG 호출 → 트랜잭션 2 (결제 APPROVED + 주문 PAID)
 * </pre>
 */
class PaymentTransactionBoundaryIT extends ContextTest {

    @MockitoBean
    private PgClient pgClient;

    private final MemberWriter memberWriter;

    private final OrderUseCase orderUseCase;

    private final OrderReader orderReader;

    private final OrderWriter orderWriter;

    private final PaymentUseCase paymentUseCase;

    private final EntityManager entityManager;

    PaymentTransactionBoundaryIT(MemberWriter memberWriter, OrderUseCase orderUseCase, OrderReader orderReader,
            OrderWriter orderWriter, PaymentUseCase paymentUseCase, EntityManager entityManager) {
        this.memberWriter = memberWriter;
        this.orderUseCase = orderUseCase;
        this.orderReader = orderReader;
        this.orderWriter = orderWriter;
        this.paymentUseCase = paymentUseCase;
        this.entityManager = entityManager;
    }

    @Test
    void pgFailureLeavesFailedPaymentAndUnpaidOrder() {
        Long orderId = placeOrder("pg-fail@example.com");
        when(pgClient.approve(anyString(), anyLong()))
            .thenThrow(new PgApprovalException("declined", new RuntimeException()));

        assertThatThrownBy(() -> paymentUseCase.pay(new PaymentPayCommand(orderId, PaymentMethod.CARD)))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FAILED));

        assertThat(paymentStatuses(orderId)).containsExactly(PaymentStatus.FAILED.name());
        assertThat(orderReader.find(orderId).getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void failureAfterPgApprovalRollsBackApprovalButKeepsRequestedPayment() {
        Long orderId = placeOrder("tx2-fail@example.com");
        // PG 승인 사이에 주문이 취소되어 트랜잭션 2 의 markPaid 가 거절되는 상황
        when(pgClient.approve(anyString(), anyLong())).thenAnswer(invocation -> {
            orderWriter.cancel(orderId);
            return new PgApproval("tx-1");
        });

        assertThatThrownBy(() -> paymentUseCase.pay(new PaymentPayCommand(orderId, PaymentMethod.CARD)))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_PAYABLE));

        // 트랜잭션 2 의 승인은 함께 롤백되고, 트랜잭션 1 이 남긴 REQUESTED 는 대사 대상으로 남는다
        assertThat(paymentStatuses(orderId)).containsExactly(PaymentStatus.REQUESTED.name());
    }

    private Long placeOrder(String email) {
        Long memberId = memberWriter.register(Email.of(email), "kim");
        return orderUseCase
            .place(new OrderPlaceCommand(memberId, "keyboard", 1, Money.of(10_000),
                    new Address("Seoul", "Teheran-ro 1", "06000")))
            .getId();
    }

    @SuppressWarnings("unchecked")
    private List<String> paymentStatuses(Long orderId) {
        return entityManager.createNativeQuery("select status from payment where order_id = ?1")
            .setParameter(1, orderId)
            .getResultList();
    }

}
