package com.meteor.storage;

import com.meteor.enums.OrderStatus;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 주문 영속 모델. 도메인을 모르고 core-enum 의 enum 만 안다. 값 객체(Money, Address)는 컬럼으로 펼쳐 저장한다. 회원은
 * memberId 로만 참조하고 연관관계를 걸지 않는다.
 */
@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long unitPrice;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 200)
    private String street;

    @Column(nullable = false, length = 20)
    private String zipCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    protected OrderEntity() {
    }

    public OrderEntity(Long memberId, String productName, int quantity, long unitPrice, String city, String street,
            String zipCode, OrderStatus status) {
        this.memberId = memberId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.city = city;
        this.street = street;
        this.zipCode = zipCode;
        this.status = status;
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
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

    public long getUnitPrice() {
        return unitPrice;
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public String getZipCode() {
        return zipCode;
    }

    public OrderStatus getStatus() {
        return status;
    }

}
