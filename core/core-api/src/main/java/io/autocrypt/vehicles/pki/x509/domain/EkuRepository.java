package io.autocrypt.vehicles.pki.x509.domain;

import java.util.Collection;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.storage.EkuEntity;
import io.autocrypt.vehicles.pki.x509.storage.EkuJpaRepository;

import org.springframework.stereotype.Repository;

/**
 * EKU 저장소 어댑터. 엔티티는 이 클래스 밖으로 나가지 않는다(R-05). EKU 는 등록 후 바뀌지 않으므로 새 EKU 만 저장한다.
 */
@Repository
public class EkuRepository {

    private final EkuJpaRepository jpa;

    EkuRepository(EkuJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Eku save(Eku eku) {
        if (!eku.isNew()) {
            throw new IllegalStateException("EKU is immutable: " + eku.getId());
        }
        return toDomain(jpa.save(new EkuEntity(eku.getOid(), eku.getPurpose())));
    }

    public boolean existsByOid(String oid) {
        return jpa.existsByOid(oid);
    }

    public List<Eku> findAllByOids(Collection<String> oids) {
        return jpa.findAllByOidIn(oids).stream().map(EkuRepository::toDomain).toList();
    }

    /** 등록 순서(id 오름차순). */
    public List<Eku> findAll() {
        return jpa.findAllByOrderByIdAsc().stream().map(EkuRepository::toDomain).toList();
    }

    private static Eku toDomain(EkuEntity entity) {
        return Eku.restore(entity.getId(), entity.getOid(), entity.getPurpose());
    }

}
