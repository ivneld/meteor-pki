package com.meteor.pki.api.response;

import java.time.Instant;
import java.util.List;

import com.meteor.pki.domain.Cert;
import com.meteor.pki.enums.KeyUsage;

public record LeafP10CrIssuanceResponse(byte[] x509CertificateDer, String issuerDn, String subjectDn, Instant notBefore,
        Instant notAfter, String serialNumberHex, String akiHex, String skiHex, String signatureAlgorithmName,
        String signatureAlgorithmOid, List<KeyUsage> keyUsages, List<String> extendedKeyUsages) {

    public static LeafP10CrIssuanceResponse from(Cert cert) {
        return new LeafP10CrIssuanceResponse(cert.getDer(), cert.getIssuerDn().value(), cert.getSubjectDn().value(),
                cert.getNotBefore(), cert.getNotAfter(), cert.getSerialNumber().toHex(),
                cert.getAuthorityKeyId().toHex(), cert.getSubjectKeyId().toHex(),
                cert.getSignatureAlgorithm().getJcaName(), cert.getSignatureAlgorithm().getOid(), cert.getKeyUsages(),
                cert.getExtendedKeyUsages());
    }

}
