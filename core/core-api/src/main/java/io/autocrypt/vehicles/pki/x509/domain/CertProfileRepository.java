package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;
import java.util.Optional;

import io.autocrypt.vehicles.pki.x509.storage.CertProfileEkuValue;
import io.autocrypt.vehicles.pki.x509.storage.CertProfileEntity;
import io.autocrypt.vehicles.pki.x509.storage.CertProfileJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * 인증서 프로파일 저장소 어댑터. 엔티티는 이 클래스 밖으로 나가지 않는다(R-05). 프로파일은 만든 뒤 바뀌지 않으므로 새 프로파일만 저장한다. 컬렉션은
 * 지연 로딩이라 호출자의 트랜잭션(*Reader / *Writer) 안에서 애그리거트로 바꾼다.
 */
@Repository
public class CertProfileRepository {

    private final CertProfileJpaRepository jpa;

    CertProfileRepository(CertProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    public CertProfile save(CertProfile profile) {
        if (!profile.isNew()) {
            throw new IllegalStateException("Certificate profile is immutable: " + profile.getId());
        }
        CertificatePolicies policies = profile.getCertificatePolicies();
        CertProfileEntity entity = new CertProfileEntity(profile.getName(), profile.getCertType(), profile.isCa(),
                profile.getPathLenConstraint(), profile.isKeyUsageCritical(), policies != null && policies.critical(),
                profile.getKeyAlgorithm().spec(), profile.getSubjectAltNameTemplate().csv(),
                profile.getValidityDuration().unit(), profile.getValidityDuration().amount(),
                profile.isRequireApproval(), profile.isUseDefaultAiaCaIssuer(), profile.getKeyUsages(),
                profile.getExtendedKeyUsages()
                    .stream()
                    .map(eku -> new CertProfileEkuValue(eku.oid(), eku.purpose()))
                    .toList(),
                policies == null ? List.of() : policies.oids(), profile.getOcspUris());
        return toDomain(jpa.save(entity));
    }

    public Optional<CertProfile> findById(Long id) {
        return jpa.findById(id).map(CertProfileRepository::toDomain);
    }

    public Optional<CertProfile> findByName(String name) {
        return jpa.findByName(name).map(CertProfileRepository::toDomain);
    }

    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    /** 만든 순서(id 오름차순)로 자른다. */
    public CertProfilePage findPage(PageQuery query) {
        Page<CertProfileEntity> result = jpa
            .findAll(PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.ASC, "id")));
        return new CertProfilePage(result.getContent().stream().map(CertProfileRepository::toDomain).toList(),
                query.page(), query.size(), result.getTotalElements(), result.getTotalPages());
    }

    private static CertProfile toDomain(CertProfileEntity entity) {
        List<String> policyOids = entity.getPolicyOids();
        CertificatePolicies policies = policyOids.isEmpty() ? null
                : new CertificatePolicies(policyOids, entity.isCertificatePoliciesCritical());
        return CertProfile.restore(entity.getId(), entity.getName(), entity.getCertType(),
                new ValidityDuration(entity.getValidityUnit(), entity.getValidityAmount()),
                entity.getPathLenConstraint(), entity.getKeyUsages(), entity.isKeyUsageCritical(),
                entity.getExtendedKeyUsages()
                    .stream()
                    .map(value -> new ProfileEku(value.getOid(), value.getPurpose()))
                    .toList(),
                policies, new SubjectAltNameTemplate(entity.getSanCsv()), new KeyAlgorithm(entity.getKeySpec()),
                entity.isRequireApproval(), entity.isUseDefaultAiaCaIssuer(), List.copyOf(entity.getOcspUris()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

}
