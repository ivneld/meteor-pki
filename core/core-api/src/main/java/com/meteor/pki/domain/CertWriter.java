package com.meteor.pki.domain;

import java.time.Clock;

import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.CrlReason;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CertWriter {

    private final CertRepository certRepository;

    private final Clock clock;

    public CertWriter(CertRepository certRepository, Clock clock) {
        this.certRepository = certRepository;
        this.clock = clock;
    }

    public Cert register(Cert issued) {
        return certRepository.save(issued);
    }

    public Cert revoke(KeyIdentifier authorityKeyId, SerialNumber serialNumber, CertType expected, CrlReason reason) {
        Cert cert = certRepository.findBySerialNumber(authorityKeyId, serialNumber)
            .orElseThrow(() -> new CoreException(ErrorCode.CERT_RESOURCE_NOT_FOUND, "Certificate not found.")
                .property("akiHex", authorityKeyId.toHex())
                .property("serialNumberHex", serialNumber.toHex()));
        return certRepository.save(cert.revoke(expected, reason, clock.instant()));
    }

}
