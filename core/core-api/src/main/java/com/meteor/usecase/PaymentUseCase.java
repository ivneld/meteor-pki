package com.meteor.usecase;

import com.meteor.client.pg.PgApproval;
import com.meteor.client.pg.PgApprovalException;
import com.meteor.client.pg.PgClient;
import com.meteor.domain.OrderReader;
import com.meteor.domain.OrderWriter;
import com.meteor.domain.Payment;
import com.meteor.domain.PaymentWriter;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 결제 유스케이스. 트랜잭션을 의도적으로 나누는 본보기다.
 *
 * <p>
 * PG 호출 동안 DB 트랜잭션을 잡고 있지 않도록 메서드에 {@code @Transactional} 을 붙이지 않고
 * {@link TransactionTemplate} 으로 구간을 직접 감싼다. 같은 클래스의 {@code @Transactional} 메서드를
 * {@code this} 로 부르면 프록시를 거치지 않아 트랜잭션이 적용되지 않으므로 그 방식은 쓰지 않는다.
 */
@Service
public class PaymentUseCase {

    private final OrderReader orderReader;

    private final OrderWriter orderWriter;

    private final PaymentWriter paymentWriter;

    private final PgClient pgClient;

    private final TransactionTemplate transaction;

    public PaymentUseCase(OrderReader orderReader, OrderWriter orderWriter, PaymentWriter paymentWriter,
            PgClient pgClient, TransactionTemplate transaction) {
        this.orderReader = orderReader;
        this.orderWriter = orderWriter;
        this.paymentWriter = paymentWriter;
        this.pgClient = pgClient;
        this.transaction = transaction;
    }

    /**
     * <pre>
     * 트랜잭션 1   결제 금액 확인 + 결제 REQUESTED 저장
     * 트랜잭션 밖  PG 승인 요청
     * 트랜잭션 2   결제 APPROVED + 주문 PAID   (함께 커밋·롤백)
     * </pre>
     *
     * 트랜잭션 1 이 커밋된 뒤 PG 가 거절하면 결제를 FAILED 로 남긴다. PG 승인 뒤 트랜잭션 2 가 실패하면 결제는 REQUESTED 로
     * 남으므로 PG 거래와 대사해 처리한다. 나눈 구간마다 이런 중간 상태가 의미를 갖도록 설계한다(S-06).
     */
    public Payment pay(PaymentPayCommand command) {
        Payment requested = transaction.execute(status -> paymentWriter.request(command.orderId(),
                orderReader.payableAmount(command.orderId()), command.method()));

        PgApproval approval;
        try {
            approval = pgClient.approve(requested.paymentKey(), requested.getAmount().amount());
        }
        catch (PgApprovalException ex) {
            paymentWriter.fail(requested.getId());
            CoreException failure = new CoreException(ErrorCode.PAYMENT_FAILED).property("paymentId",
                    requested.getId());
            failure.initCause(ex);
            throw failure;
        }

        return transaction.execute(status -> {
            Payment approved = paymentWriter.approve(requested.getId(), approval.transactionKey());
            orderWriter.markPaid(command.orderId());
            return approved;
        });
    }

}
