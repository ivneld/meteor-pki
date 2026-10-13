package io.autocrypt.vehicles.pki.x509.domain;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증서 조회.
 */
@Component
@Transactional(readOnly = true)
public class CertReader {

    private final CertRepository certRepository;

    public CertReader(CertRepository certRepository) {
        this.certRepository = certRepository;
    }

    /**
     * 발급 요청의 AKI(발급자 SKI)로 발급자 CA 를 찾고, target 종류를 지금 발급할 수 있는지 애그리거트에 확인받는다.
     */
    public Cert getIssuer(KeyIdentifier authorityKeyId, CertType target, Instant now) {
        Cert issuer = certRepository.findLatestValidCa(authorityKeyId)
            .orElseThrow(() -> new CoreException(ErrorCode.CERT_RESOURCE_NOT_FOUND,
                    "Failed to find a CA certificate matching the AKI.")
                .property("akiHex", authorityKeyId.toHex()));
        issuer.ensureCanIssue(target, now);
        return issuer;
    }

    public Cert getCa(KeyIdentifier subjectKeyId) {
        return certRepository.findLatestValidCa(subjectKeyId)
            .orElseThrow(() -> new CoreException(ErrorCode.CERT_RESOURCE_NOT_FOUND,
                    "Failed to find a CA certificate matching the SKI.")
                .property("skiHex", subjectKeyId.toHex()));
    }

    /** 체인마다 가장 최근의 CA 인증서. 숨긴 인증서는 빼고, 폐기·만료된 인증서도 포함한다. */
    public List<Cert> getLatestCasPerChain() {
        return CaCertificates.latestPerChain(certRepository.findVisibleCas());
    }

    public Cert get(KeyIdentifier authorityKeyId, SerialNumber serialNumber) {
        return certRepository.findBySerialNumber(authorityKeyId, serialNumber)
            .orElseThrow(() -> new CoreException(ErrorCode.CERT_RESOURCE_NOT_FOUND, "Certificate not found.")
                .property("akiHex", authorityKeyId.toHex())
                .property("serialNumberHex", serialNumber.toHex()));
    }

}
