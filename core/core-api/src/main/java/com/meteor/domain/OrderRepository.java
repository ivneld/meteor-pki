package com.meteor.domain;

import java.util.Optional;

import com.meteor.enums.OrderStatus;
import com.meteor.storage.OrderEntity;
import com.meteor.storage.OrderJpaRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

/**
 * 주문 저장소 어댑터. storage 모듈의 JPA 엔티티를 애그리거트로 바꾸고, 그 반대도 한다. 엔티티는 이 클래스 밖으로 나가지 않는다(R-05).
 *
 * <p>
 * 더티 체킹에 기대지 않는다. 애그리거트를 바꿨으면 {@link #save(Order)} 를 명시적으로 호출해야 한다(S-05).
 */
@Repository
public class OrderRepository {

    private final OrderJpaRepository jpa;

    OrderRepository(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Order> findById(Long id) {
        return jpa.findById(id).map(OrderRepository::toDomain);
    }

    public Order save(Order order) {
        OrderEntity entity;
        if (order.isNew()) {
            Address address = order.getShippingAddress();
            entity = new OrderEntity(order.getMemberId(), order.getProductName(), order.getQuantity(),
                    order.getUnitPrice().amount(), address.city(), address.street(), address.zipCode(),
                    order.getStatus());
        }
        else {
            entity = jpa.findById(order.getId())
                .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", order.getId()));
            entity.changeStatus(order.getStatus());
        }
        return toDomain(jpa.save(entity));
    }

    public long countByMemberIdAndStatus(Long memberId, OrderStatus status) {
        return jpa.countByMemberIdAndStatus(memberId, status);
    }

    private static Order toDomain(OrderEntity entity) {
        return Order.restore(entity.getId(), entity.getMemberId(), entity.getProductName(), entity.getQuantity(),
                Money.of(entity.getUnitPrice()), new Address(entity.getCity(), entity.getStreet(), entity.getZipCode()),
                entity.getStatus());
    }

}
