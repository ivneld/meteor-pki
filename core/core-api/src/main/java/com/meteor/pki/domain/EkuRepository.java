package com.meteor.pki.domain;

import java.util.Collection;
import java.util.List;

import com.meteor.pki.storage.EkuEntity;
import com.meteor.pki.storage.EkuJpaRepository;

import org.springframework.stereotype.Repository;

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

    public List<Eku> findAll() {
        return jpa.findAllByOrderByIdAsc().stream().map(EkuRepository::toDomain).toList();
    }

    private static Eku toDomain(EkuEntity entity) {
        return Eku.restore(entity.getId(), entity.getOid(), entity.getPurpose());
    }

}
