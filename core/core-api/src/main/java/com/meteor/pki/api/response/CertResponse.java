package com.meteor.pki.api.response;

import java.time.Instant;
import java.util.List;

import com.meteor.pki.domain.Cert;
import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;

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
