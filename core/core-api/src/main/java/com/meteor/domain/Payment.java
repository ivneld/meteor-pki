package com.meteor.domain;

import com.meteor.enums.PaymentMethod;
import com.meteor.enums.PaymentStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

/**
 * 결제 애그리거트. 주문은 orderId 로만 참조한다. 상태 기계: REQUESTED → APPROVED | FAILED.
 *
 * <p>
 * PG 호출 전에 REQUESTED 로 먼저 저장하므로, PG 승인 뒤 처리가 실패해도 "요청은 했지만 확정되지 않은 결제"가 남아 대사(reconcile)할
 * 수 있다. 트랜잭션을 나누는 유스케이스는 이렇게 중간 상태가 의미를 갖도록 설계한다.
 */
public class Payment {

    private final Long id;

    private final Long orderId;

    private final Money amount;

    private final PaymentMethod method;

    private PaymentStatus status;

    private String pgTransactionKey;

    private Payment(Long id, Long orderId, Money amount, PaymentMethod method, PaymentStatus status,
            String pgTransactionKey) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.pgTransactionKey = pgTransactionKey;
    }

    public static Payment request(Long orderId, Money amount, PaymentMethod method) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
        return new Payment(null, orderId, amount, method, PaymentStatus.REQUESTED, null);
    }

    /** 저장소에서 복원한다. PaymentRepository 만 호출한다. */
    public static Payment restore(Long id, Long orderId, Money amount, PaymentMethod method, PaymentStatus status,
            String pgTransactionKey) {
        return new Payment(id, orderId, amount, method, status, pgTransactionKey);
    }

    public void approve(String pgTransactionKey) {
        ensureRequested();
        this.status = PaymentStatus.APPROVED;
        this.pgTransactionKey = pgTransactionKey;
    }

    public void fail() {
        ensureRequested();
        this.status = PaymentStatus.FAILED;
    }

    /** PG 에 넘기는 멱등 키. 같은 결제는 같은 키로 요청한다. */
    public String paymentKey() {
        return String.valueOf(id);
    }

    private void ensureRequested() {
        if (status != PaymentStatus.REQUESTED) {
            throw new CoreException(ErrorCode.PAYMENT_NOT_REQUESTED).property("paymentId", id)
                .property("paymentStatus", status);
        }
    }

    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPgTransactionKey() {
        return pgTransactionKey;
    }

}
