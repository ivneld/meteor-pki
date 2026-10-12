package com.meteor.storage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

}
