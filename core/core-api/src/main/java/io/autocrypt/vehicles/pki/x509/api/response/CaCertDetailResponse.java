package io.autocrypt.vehicles.pki.x509.api.response;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.enums.CertStatus;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

public record CaCertDetailResponse(Long id, String serialNumberHex, String issuerDn, String subjectDn,
        String keyAlgorithm, Instant notBefore, Instant notAfter, String signatureAlgorithmName,
        String signatureAlgorithmOid, CertType certType, CertStatus certStatus, String akiHex, String skiHex,
        List<KeyUsage> keyUsages, List<String> extendedKeyUsages, List<CustomOidExtensionResponse> customOidExtensions,
        String trackingId, String certPem, CertDescriptionContentResponse certDescription) {
}
