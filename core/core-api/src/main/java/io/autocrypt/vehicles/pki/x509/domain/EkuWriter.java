package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * EKU 등록. 같은 OID 를 두 번 등록할 수 없다는 판단은 {@link EkuRegistrationPolicy} 가 한다.
 */
@Component
@Transactional
public class EkuWriter {

    private final EkuRepository ekuRepository;

    public EkuWriter(EkuRepository ekuRepository) {
        this.ekuRepository = ekuRepository;
    }

    public Eku register(String oid, String purpose) {
        Eku eku = Eku.register(oid, purpose);
        EkuRegistrationPolicy.ensureNotRegistered(oid, ekuRepository.existsByOid(oid));
        return ekuRepository.save(eku);
    }

    /** 표준 EKU 중 아직 없는 것만 등록한다. 기동할 때마다 불러도 결과가 같다. */
    public void registerDefaults() {
        List<String> registered = ekuRepository.findAll().stream().map(Eku::getOid).toList();
        EkuRegistrationPolicy.missingDefaults(registered).forEach(ekuRepository::save);
    }

}
