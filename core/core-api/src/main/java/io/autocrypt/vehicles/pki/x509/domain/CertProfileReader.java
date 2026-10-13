package io.autocrypt.vehicles.pki.x509.domain;

import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증서 프로파일 조회.
 */
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

    /** target 종류를 발급하는 프로파일. 종류가 다르면 없는 프로파일과 같이 404 다. */
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

    /** 이 서버는 모든 키 알고리즘으로 발급할 수 있으므로 모든 프로파일이 발급 가능하다. */
    public CertProfilePage getPage(PageQuery query) {
        return certProfileRepository.findPage(query);
    }

}
