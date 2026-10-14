package com.meteor.pki.domain;

import java.util.List;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CertReader {

    private final CertRepository certRepository;

    public CertReader(CertRepository certRepository) {
        this.certRepository = certRepository;
    }

    public Cert getIssuer(KeyIdentifier authorityKeyId) {
        return certRepository.findLatestValidCa(authorityKeyId)
            .orElseThrow(() -> new CoreException(ErrorCode.CERT_RESOURCE_NOT_FOUND,
                    "Failed to find a CA certificate matching the AKI.")
                .property("akiHex", authorityKeyId.toHex()));
    }

    public Cert getCa(KeyIdentifier subjectKeyId) {
        return certRepository.findLatestValidCa(subjectKeyId)
            .orElseThrow(() -> new CoreException(ErrorCode.CERT_RESOURCE_NOT_FOUND,
                    "Failed to find a CA certificate matching the SKI.")
                .property("skiHex", subjectKeyId.toHex()));
    }

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
