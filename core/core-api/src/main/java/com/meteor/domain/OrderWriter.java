package com.meteor.domain;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 생성과 상태 변경. 꺼내고, 애그리거트와 Policy 에 넘기고, 저장한다.
 *
 * <p>
 * 다룰 수 있는 것은 주문 애그리거트와 {@link OrderRepository} 뿐이다(R-04). 회원 확인처럼 다른 애그리거트가 필요한 흐름은
 * usecase 가 엮는다({@code OrderUseCase.place}). 주문 한 건으로 끝나는 흐름(취소)은 api 가 이 클래스를 바로 부른다.
 */
@Component
@Transactional
public class OrderWriter {

    private final OrderRepository orderRepository;

    public OrderWriter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /** 건수라는 사실은 여기서 꺼내고, 한도 판단은 {@link OrderLimitPolicy} 가 한다(S-04). */
    public Order place(Long memberId, String productName, int quantity, Money unitPrice, Address shippingAddress) {
        long awaiting = orderRepository.countByMemberIdAndStatus(memberId, OrderLimitPolicy.COUNTED_STATUS);
        OrderLimitPolicy.ensureCanPlace(memberId, awaiting);
        return orderRepository.save(Order.place(memberId, productName, quantity, unitPrice, shippingAddress));
    }

    public Order cancel(Long orderId) {
        Order order = load(orderId);
        order.cancel();
        return orderRepository.save(order);
    }

    public Order markPaid(Long orderId) {
        Order order = load(orderId);
        order.markPaid();
        return orderRepository.save(order);
    }

    private Order load(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", orderId));
    }

}
