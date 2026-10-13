package io.autocrypt.vehicles.pki.x509.api.response;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.domain.Cert;
import io.autocrypt.vehicles.pki.x509.enums.CertStatus;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

/**
 * certStatus 는 조회 시점 기준이라 유효기간이 지났으면 EXPIRED 다. 사용자 정의 확장은 아직 발급하지 않으므로 빈 목록이다.
 */
public record CertResponse(Long id, String serialNumberHex, String issuerDn, String subjectDn, String keyAlgorithm,
        Instant notBefore, Instant notAfter, String signatureAlgorithmName, String signatureAlgorithmOid,
        CertType certType, CertStatus certStatus, String akiHex, String skiHex, List<KeyUsage> keyUsages,
        List<String> extendedKeyUsages, List<CustomOidExtensionResponse> customOidExtensions) {

    public static CertResponse from(Cert cert, Instant now) {
        return new CertResponse(cert.getId(), cert.getSerialNumber().toHex(), cert.getIssuerDn().value(),
                cert.getSubjectDn().value(), cert.getKeyAlgorithm().value(), cert.getNotBefore(), cert.getNotAfter(),
                cert.getSignatureAlgorithm().getJcaName(), cert.getSignatureAlgorithm().getOid(), cert.getCertType(),
                cert.getStatusAt(now), cert.getAuthorityKeyId().toHex(), cert.getSubjectKeyId().toHex(),
                cert.getKeyUsages(), cert.getExtendedKeyUsages(), List.of());
    }

}
