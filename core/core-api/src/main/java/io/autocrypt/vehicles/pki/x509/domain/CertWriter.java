package io.autocrypt.vehicles.pki.x509.domain;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발급한 인증서 저장. 인증서를 만드는 일(프로파일 확인, 키 생성, 서명)은 여러 애그리거트와 키 보관소를 엮으므로
 * {@code CertIssuanceUseCase} 가 한다.
 */
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
