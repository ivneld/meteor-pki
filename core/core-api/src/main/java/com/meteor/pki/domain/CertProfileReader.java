package com.meteor.pki.domain;

import com.meteor.pki.enums.CertType;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CertProfileReader {

    private final CertProfileRepository certProfileRepository;

    public CertProfileReader(CertProfileRepository certProfileRepository) {
        this.certProfileRepository = certProfileRepository;
    }

    public CertProfile get(Long id) {
        return certProfileRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorCode.PROFILE_RESOURCE_NOT_FOUND, "Certificate profile not found.")
                .property("certProfileId", id));
    }

    public CertProfile getForIssuance(Long id, CertType target) {
        CertProfile profile = get(id);
        profile.ensureIssues(target);
        return profile;
    }

    public CertProfile getByName(String name) {
        return certProfileRepository.findByName(name)
            .orElseThrow(() -> new CoreException(ErrorCode.PROFILE_RESOURCE_NOT_FOUND, "Certificate profile not found.")
                .property("profileName", name));
    }

    public CertProfilePage getPage(PageQuery query) {
        return certProfileRepository.findPage(query);
    }

}
