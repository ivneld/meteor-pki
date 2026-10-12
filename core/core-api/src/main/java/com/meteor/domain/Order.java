package com.meteor.domain;

import com.meteor.enums.OrderStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

/**
 * 주문 애그리거트. 회원은 memberId 로만 참조한다. 상태 전이 규칙은 모두 여기 있다.
 */
public class Order {

    private final Long id;

    private final Long memberId;

    private final String productName;

    private final int quantity;

    private final Money unitPrice;

    private final Address shippingAddress;

    private OrderStatus status;

    private Order(Long id, Long memberId, String productName, int quantity, Money unitPrice, Address shippingAddress,
            OrderStatus status) {
        this.id = id;
        this.memberId = memberId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.shippingAddress = shippingAddress;
        this.status = status;
    }

    public static Order place(Long memberId, String productName, int quantity, Money unitPrice,
            Address shippingAddress) {
        if (quantity < 1) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "quantity must be at least 1");
        }
        return new Order(null, memberId, productName, quantity, unitPrice, shippingAddress, OrderStatus.CREATED);
    }

    /** 저장소에서 복원한다. OrderRepository 만 호출한다. */
    public static Order restore(Long id, Long memberId, String productName, int quantity, Money unitPrice,
            Address shippingAddress, OrderStatus status) {
        return new Order(id, memberId, productName, quantity, unitPrice, shippingAddress, status);
    }

    public Money totalAmount() {
        return unitPrice.times(quantity);
    }

    /** 결제할 금액. 결제 가능한 상태가 아니면 거절한다. */
    public Money payableAmount() {
        ensurePayable();
        return totalAmount();
    }

    public void markPaid() {
        ensurePayable();
        status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status != OrderStatus.CREATED) {
            throw new CoreException(ErrorCode.ORDER_NOT_CANCELLABLE).property("orderId", id)
                .property("orderStatus", status);
        }
        status = OrderStatus.CANCELLED;
    }

    private void ensurePayable() {
        if (status != OrderStatus.CREATED) {
            throw new CoreException(ErrorCode.ORDER_NOT_PAYABLE).property("orderId", id)
                .property("orderStatus", status);
        }
    }

    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

}
