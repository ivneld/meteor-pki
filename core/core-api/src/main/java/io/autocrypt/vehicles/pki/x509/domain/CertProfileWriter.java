package io.autocrypt.vehicles.pki.x509.domain;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증서 프로파일 생성. 종류별 규칙은 {@link CertProfile#create}, 이름 중복은 {@link CertProfileNamePolicy} 가
 * 판단하고, 여기서는 사실을 꺼내 넘기고 저장한다. 등록된 EKU 를 찾는 일은 다른 애그리거트라 usecase 가
 * 엮는다({@code CertProfileUseCase.create}).
 */
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
