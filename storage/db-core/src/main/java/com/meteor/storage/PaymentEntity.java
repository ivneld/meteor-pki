package com.meteor.storage;

import com.meteor.enums.PaymentMethod;
import com.meteor.enums.PaymentStatus;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 결제 영속 모델. 주문은 orderId 로만 참조한다.
 */
@Entity
@Table(name = "payment")
public class PaymentEntity extends BaseEntity {

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 100)
    private String pgTransactionKey;

    protected PaymentEntity() {
    }

    public PaymentEntity(Long orderId, long amount, PaymentMethod method, PaymentStatus status,
            String pgTransactionKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.pgTransactionKey = pgTransactionKey;
    }

    public void changeStatus(PaymentStatus status, String pgTransactionKey) {
        this.status = status;
        this.pgTransactionKey = pgTransactionKey;
    }

    public Long getOrderId() {
        return orderId;
    }

    public long getAmount() {
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
