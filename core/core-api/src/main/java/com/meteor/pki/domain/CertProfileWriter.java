package com.meteor.pki.domain;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CertProfileWriter {

    private final CertProfileRepository certProfileRepository;

    public CertProfileWriter(CertProfileRepository certProfileRepository) {
        this.certProfileRepository = certProfileRepository;
    }

    public CertProfile create(CertProfileDraft draft) {
        CertProfileNamePolicy.ensureUnique(draft.name(), certProfileRepository.existsByName(draft.name()));
        return certProfileRepository.save(CertProfile.create(draft));
    }

}
