package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;
import java.util.Optional;

import io.autocrypt.vehicles.pki.x509.enums.CertStatus;
import io.autocrypt.vehicles.pki.x509.storage.CertEntity;
import io.autocrypt.vehicles.pki.x509.storage.CertJpaRepository;

import org.springframework.stereotype.Repository;

/**
 * 인증서 저장소 어댑터. 엔티티는 이 클래스 밖으로 나가지 않는다(R-05). 조회 조건 컬럼은 저장할 때 인증서에서 꺼내 채운다.
 */
@Repository
public class CertRepository {

    private final CertJpaRepository jpa;

    CertRepository(CertJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** 새로 발급한 인증서만 저장한다. 상태를 바꾸는 기능(폐기, 승인)은 아직 없다. */
    public Cert save(Cert cert) {
        if (!cert.isNew()) {
            throw new IllegalStateException("Certificate status changes are not supported yet: " + cert.getId());
        }
        PrivateKeyId privateKeyId = cert.getPrivateKeyId();
        CertEntity entity = new CertEntity(cert.getSerialNumber().toHex(), cert.getIssuerDn().value(),
                cert.getSubjectDn().value(), cert.getNotBefore(), cert.getNotAfter(), cert.getSubjectKeyId().toHex(),
                cert.getAuthorityKeyId().toHex(), cert.getPathLenConstraint(),
                privateKeyId == null ? null : privateKeyId.value(), cert.getCertType(), cert.getStatus(),
                cert.getKeyAlgorithm().spec(), cert.getSignatureAlgorithm(), cert.getTrackingId(), cert.isHidden(),
                cert.getDer());
        return toDomain(jpa.save(entity));
    }

    /** SKI 가 같은 VALID CA 인증서 중 가장 최근 것. */
    public Optional<Cert> findLatestValidCa(KeyIdentifier subjectKeyId) {
        return jpa
            .findFirstBySkiAndCertTypeInAndStatusOrderByCreatedAtDescIdDesc(subjectKeyId.toHex(), Cert.CA_TYPES,
                    CertStatus.VALID)
            .map(CertRepository::toDomain);
    }

    /** 숨기지 않은 모든 CA 인증서. 상태는 가리지 않는다. */
    public List<Cert> findVisibleCas() {
        return jpa.findAllByCertTypeInAndHiddenFalse(Cert.CA_TYPES).stream().map(CertRepository::toDomain).toList();
    }

    public Optional<Cert> findBySerialNumber(KeyIdentifier authorityKeyId, SerialNumber serialNumber) {
        return jpa.findBySerialNumberAndAki(serialNumber.toHex(), authorityKeyId.toHex()).map(CertRepository::toDomain);
    }

    private static Cert toDomain(CertEntity entity) {
        return Cert.restore(entity.getId(), entity.getDer(), entity.getCertType(), entity.getStatus(),
                entity.getPrivateKeyId() == null ? null : new PrivateKeyId(entity.getPrivateKeyId()), entity.isHidden(),
                entity.getCreatedAt());
    }

}
