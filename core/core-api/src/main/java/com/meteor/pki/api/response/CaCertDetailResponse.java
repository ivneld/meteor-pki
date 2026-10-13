package com.meteor.pki.api.response;

import java.time.Instant;
import java.util.List;

import com.meteor.pki.domain.Cert;
import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;

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
