package com.meteor.storage;

import com.meteor.enums.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    long countByMemberIdAndStatus(Long memberId, OrderStatus status);

}
