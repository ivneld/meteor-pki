package com.meteor.pki.storage;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertProfileJpaRepository extends JpaRepository<CertProfileEntity, Long> {

    boolean existsByName(String name);

    Optional<CertProfileEntity> findByName(String name);

}
