package io.autocrypt.vehicles.pki.x509.api.response;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

public record LeafP10CrIssuanceResponseV1(byte[] x509CertificateDer, String issuerDn, String subjectDn,
        Instant notBefore, Instant notAfter, String serialNumberHex, String akiHex, String skiHex,
        String signatureAlgorithmName, String signatureAlgorithmOid, List<KeyUsage> keyUsages,
        List<String> extendedKeyUsages) {
}
