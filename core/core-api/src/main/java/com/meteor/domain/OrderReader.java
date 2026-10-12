package com.meteor.domain;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 조회. 읽기 전용 트랜잭션에서 돌고, 호출자에게 트랜잭션이 있으면 거기에 참여한다(R-07).
 */
@Component
@Transactional(readOnly = true)
public class OrderReader {

    private final OrderRepository orderRepository;

    public OrderReader(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order find(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", orderId));
    }

    /** 결제할 금액. 결제할 수 없는 주문이면 애그리거트가 거절한다. */
    public Money payableAmount(Long orderId) {
        return find(orderId).payableAmount();
    }

}
