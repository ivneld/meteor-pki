package io.autocrypt.vehicles.pki.x509.api.response;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.enums.CertStatus;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

public record CaCertDetailResponseV1(Long id, String serialNumberHex, String issuerDn, String subjectDn,
        Instant notBefore, Instant notAfter, String signatureAlgorithmName, String signatureAlgorithmOid,
        CertType certType, CertStatus certStatus, String akiHex, String skiHex, List<KeyUsage> keyUsages,
        List<String> extendedKeyUsages, String certPem, CertDescriptionContentResponseV1 certDescription) {
}
