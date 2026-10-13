package com.meteor.pki.storage;

import java.time.Instant;

import com.meteor.pki.enums.CertStatus;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyAlgorithmSpec;
import com.meteor.pki.enums.SignatureAlgorithm;
import com.meteor.pki.support.storage.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cert",
        uniqueConstraints = @UniqueConstraint(name = "uk_cert_aki_serial", columnNames = { "aki", "serial_number" }),
        indexes = @Index(name = "idx_cert_ski", columnList = "ski"))
public class CertEntity extends BaseEntity {

    @Column(nullable = false, length = 40)
    private String serialNumber;

    @Column(nullable = false, length = 1024)
    private String issuerDn;

    @Column(nullable = false, length = 1024)
    private String subjectDn;

    @Column(nullable = false)
    private Instant notBefore;

    @Column(nullable = false)
    private Instant notAfter;

    @Column(nullable = false, length = 64)
    private String ski;

    @Column(nullable = false, length = 64)
    private String aki;

    private Integer pathLenConstraint;

    @Column(name = "priv_key_id")
    private String privateKeyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CertType certType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cert_status", nullable = false, length = 10)
    private CertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KeyAlgorithmSpec keyAlgorithm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SignatureAlgorithm signatureAlgorithm;

    @Column(nullable = false, length = 64)
    private String trackingId;

    @Column(nullable = false)
    private boolean hidden;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name = "cert_der", nullable = false)
    private byte[] der;

    protected CertEntity() {
    }

    public CertEntity(String serialNumber, String issuerDn, String subjectDn, Instant notBefore, Instant notAfter,
            String ski, String aki, Integer pathLenConstraint, String privateKeyId, CertType certType,
            CertStatus status, KeyAlgorithmSpec keyAlgorithm, SignatureAlgorithm signatureAlgorithm, String trackingId,
            boolean hidden, byte[] der) {
        this.serialNumber = serialNumber;
        this.issuerDn = issuerDn;
        this.subjectDn = subjectDn;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.ski = ski;
        this.aki = aki;
        this.pathLenConstraint = pathLenConstraint;
        this.privateKeyId = privateKeyId;
        this.certType = certType;
        this.status = status;
        this.keyAlgorithm = keyAlgorithm;
        this.signatureAlgorithm = signatureAlgorithm;
        this.trackingId = trackingId;
        this.hidden = hidden;
        this.der = der.clone();
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getSki() {
        return ski;
    }

    public String getAki() {
        return aki;
    }

    public String getPrivateKeyId() {
        return privateKeyId;
    }

    public CertType getCertType() {
        return certType;
    }

    public CertStatus getStatus() {
        return status;
    }

    public boolean isHidden() {
        return hidden;
    }

    public byte[] getDer() {
        return der.clone();
    }

}
