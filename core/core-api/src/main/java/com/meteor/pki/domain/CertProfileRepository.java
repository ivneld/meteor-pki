package com.meteor.pki.domain;

import java.util.List;
import java.util.Optional;

import com.meteor.pki.storage.CertProfileEkuValue;
import com.meteor.pki.storage.CertProfileEntity;
import com.meteor.pki.storage.CertProfileJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

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
