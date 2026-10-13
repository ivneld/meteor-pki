package io.autocrypt.vehicles.pki.x509.usecase;

import java.util.List;

import io.autocrypt.vehicles.pki.x509.domain.CertProfile;
import io.autocrypt.vehicles.pki.x509.domain.CertProfileWriter;
import io.autocrypt.vehicles.pki.x509.domain.DistributionPoints;
import io.autocrypt.vehicles.pki.x509.domain.Eku;
import io.autocrypt.vehicles.pki.x509.domain.EkuReader;
import io.autocrypt.vehicles.pki.x509.domain.ProfileEku;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증서 프로파일 유스케이스. 등록된 EKU(다른 애그리거트)를 찾아 프로파일에 복사해 넣는 생성 흐름만 여기 둔다. 프로파일 하나로 끝나는 조회는 api 가
 * {@code CertProfileReader} 를 바로 부른다.
 */
@Service
public class CertProfileUseCase {

    private final EkuReader ekuReader;

    private final CertProfileWriter certProfileWriter;

    private final DistributionPoints distributionPoints;

    public CertProfileUseCase(EkuReader ekuReader, CertProfileWriter certProfileWriter,
            DistributionPoints distributionPoints) {
        this.ekuReader = ekuReader;
        this.certProfileWriter = certProfileWriter;
        this.distributionPoints = distributionPoints;
    }

    /**
     * EKU 확인과 프로파일 저장을 한 트랜잭션으로 묶는다. 도메인 서비스 둘 이상을 부르고 그중 Writer 가 있으므로 바깥 트랜잭션이
     * 필수다(R-08).
     */
    @Transactional
    public CertProfile create(CertProfileCreateCommand command) {
        List<ProfileEku> extendedKeyUsages = ekuReader.getAllByOids(command.ekuOids())
            .stream()
            .map(Eku::toProfileEku)
            .toList();
        return certProfileWriter.create(command.toDraft(extendedKeyUsages, distributionPoints.defaultOcspUri()));
    }

}
