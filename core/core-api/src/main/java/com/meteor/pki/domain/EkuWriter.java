package com.meteor.pki.domain;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public void registerDefaults() {
        List<String> registered = ekuRepository.findAll().stream().map(Eku::getOid).toList();
        EkuRegistrationPolicy.missingDefaults(registered).forEach(ekuRepository::save);
    }

}
