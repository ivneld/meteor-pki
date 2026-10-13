package io.autocrypt.vehicles.pki.x509.storage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.autocrypt.vehicles.pki.x509.enums.CertStatus;
import io.autocrypt.vehicles.pki.x509.enums.CertType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertJpaRepository extends JpaRepository<CertEntity, Long> {

    Optional<CertEntity> findFirstBySkiAndCertTypeInAndStatusOrderByCreatedAtDescIdDesc(String ski,
            Collection<CertType> certTypes, CertStatus status);

    List<CertEntity> findAllByCertTypeInAndHiddenFalse(Collection<CertType> certTypes);

    Optional<CertEntity> findBySerialNumberAndAki(String serialNumber, String aki);

}
