package com.meteor.pki.domain;

import java.util.List;
import java.util.Optional;

import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.storage.CertEntity;
import com.meteor.pki.storage.CertJpaRepository;

import org.springframework.stereotype.Repository;

@Repository
public class CertRepository {

    private final CertJpaRepository jpa;

    CertRepository(CertJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Cert save(Cert cert) {
        if (!cert.isNew()) {
            CertEntity entity = jpa.findById(cert.getId())
                .orElseThrow(() -> new IllegalStateException("Certificate not found: " + cert.getId()));
            Revocation revocation = cert.getRevocation();
            entity.changeStatus(cert.getStatus(), revocation == null ? null : revocation.revokedAt(),
                    revocation == null ? null : revocation.reason());
            return toDomain(jpa.save(entity));
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

    public Optional<Cert> findLatestValidCa(KeyIdentifier subjectKeyId) {
        return jpa
            .findFirstBySkiAndCertTypeInAndStatusOrderByCreatedAtDescIdDesc(subjectKeyId.toHex(), Cert.CA_TYPES,
                    CertStatus.VALID)
            .map(CertRepository::toDomain);
    }

    public List<Cert> findVisibleCas() {
        return jpa.findAllByCertTypeInAndHiddenFalse(Cert.CA_TYPES).stream().map(CertRepository::toDomain).toList();
    }

    public Optional<Cert> findBySerialNumber(KeyIdentifier authorityKeyId, SerialNumber serialNumber) {
        return jpa.findBySerialNumberAndAki(serialNumber.toHex(), authorityKeyId.toHex()).map(CertRepository::toDomain);
    }

    private static Cert toDomain(CertEntity entity) {
        return Cert.restore(entity.getId(), entity.getDer(), entity.getCertType(), entity.getStatus(),
                entity.getPrivateKeyId() == null ? null : new PrivateKeyId(entity.getPrivateKeyId()), entity.isHidden(),
                entity.getCreatedAt(), entity.getRevocationReason() == null ? null
                        : new Revocation(entity.getRevocationReason(), entity.getRevokedAt()));
    }

}
