package io.autocrypt.vehicles.pki.x509.api.response;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.domain.Cert;
import io.autocrypt.vehicles.pki.x509.enums.CertStatus;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

/**
 * {@link CertResponse} 에 추적 id, PEM, 인증서 설명을 더한 것. 인증서 설명은 아직 등록할 수 없어 항상 빈 값이다.
 */
public record CaCertDetailResponse(Long id, String serialNumberHex, String issuerDn, String subjectDn,
        String keyAlgorithm, Instant notBefore, Instant notAfter, String signatureAlgorithmName,
        String signatureAlgorithmOid, CertType certType, CertStatus certStatus, String akiHex, String skiHex,
        List<KeyUsage> keyUsages, List<String> extendedKeyUsages, List<CustomOidExtensionResponse> customOidExtensions,
        String trackingId, String certPem, CertDescriptionContentResponse certDescription) {

    public static CaCertDetailResponse from(Cert cert, Instant now) {
        CertResponse base = CertResponse.from(cert, now);
        return new CaCertDetailResponse(base.id(), base.serialNumberHex(), base.issuerDn(), base.subjectDn(),
                base.keyAlgorithm(), base.notBefore(), base.notAfter(), base.signatureAlgorithmName(),
                base.signatureAlgorithmOid(), base.certType(), base.certStatus(), base.akiHex(), base.skiHex(),
                base.keyUsages(), base.extendedKeyUsages(), base.customOidExtensions(), cert.getTrackingId(),
                cert.getPem(), new CertDescriptionContentResponse(List.of()));
    }

}
