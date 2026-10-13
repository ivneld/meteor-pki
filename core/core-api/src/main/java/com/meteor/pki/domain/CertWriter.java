package com.meteor.pki.domain;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CertWriter {

    private final CertRepository certRepository;

    public CertWriter(CertRepository certRepository) {
        this.certRepository = certRepository;
    }

    public Cert register(Cert issued) {
        return certRepository.save(issued);
    }

}
