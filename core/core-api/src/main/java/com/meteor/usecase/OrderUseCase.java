package com.meteor.usecase;

import com.meteor.domain.MemberReader;
import com.meteor.domain.Order;
import com.meteor.domain.OrderWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 유스케이스. 회원과 주문, 두 애그리거트를 엮는 흐름만 여기 둔다. 주문 하나로 끝나는 조회·취소는 api 가 {@code OrderReader},
 * {@code OrderWriter} 를 바로 부른다.
 */
@Service
public class OrderUseCase {

    private final MemberReader memberReader;

    private final OrderWriter orderWriter;

    public OrderUseCase(MemberReader memberReader, OrderWriter orderWriter) {
        this.memberReader = memberReader;
        this.orderWriter = orderWriter;
    }

    /**
     * 회원 확인과 주문 생성을 한 트랜잭션으로 묶는다. 도메인 서비스 둘 이상을 부르고 그중 Writer 가 있으므로 바깥 트랜잭션이 필수다(R-08).
     * 두 도메인 서비스는 기본 전파(REQUIRED)로 이 트랜잭션에 참여한다.
     */
    @Transactional
    public Order place(OrderPlaceCommand command) {
        memberReader.ensureActive(command.memberId());
        return orderWriter.place(command.memberId(), command.productName(), command.quantity(), command.unitPrice(),
                command.shippingAddress());
    }

}
