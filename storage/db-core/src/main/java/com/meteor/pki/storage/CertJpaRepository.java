package com.meteor.pki.storage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertJpaRepository extends JpaRepository<CertEntity, Long> {

    Optional<CertEntity> findFirstBySkiAndCertTypeInAndStatusOrderByCreatedAtDescIdDesc(String ski,
            Collection<CertType> certTypes, CertStatus status);

    Optional<CertEntity> findFirstBySkiAndCertTypeInOrderByCreatedAtDescIdDesc(String ski,
            Collection<CertType> certTypes);

    Optional<CertEntity> findFirstByTrackingIdAndCertTypeInAndStatusOrderByCreatedAtDescIdDesc(String trackingId,
            Collection<CertType> certTypes, CertStatus status);

    List<CertEntity> findAllByCertTypeInAndHiddenFalse(Collection<CertType> certTypes);

    List<CertEntity> findAllByOrderByCreatedAtDescIdDesc();

    List<CertEntity> findAllByTrackingIdOrderByCreatedAtAscIdAsc(String trackingId);

    Optional<CertEntity> findBySerialNumberAndAki(String serialNumber, String aki);

}
