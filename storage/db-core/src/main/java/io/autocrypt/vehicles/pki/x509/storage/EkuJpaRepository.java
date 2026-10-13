package io.autocrypt.vehicles.pki.x509.storage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EkuJpaRepository extends JpaRepository<EkuEntity, Long> {

    Optional<EkuEntity> findByOid(String oid);

    boolean existsByOid(String oid);

    List<EkuEntity> findAllByOidIn(Collection<String> oids);

    List<EkuEntity> findAllByOrderByIdAsc();

}
