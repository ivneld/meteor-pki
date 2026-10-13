package io.autocrypt.vehicles.pki.x509.storage;

import java.time.Instant;

import io.autocrypt.vehicles.pki.x509.enums.CertStatus;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyAlgorithmSpec;
import io.autocrypt.vehicles.pki.x509.enums.SignatureAlgorithm;
import io.autocrypt.vehicles.pki.x509.support.storage.BaseEntity;
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

/**
 * 발급한 인증서 영속 모델. 인증서 DER 가 원본이고, 조회 조건으로 쓰는 값(일련번호, 키 식별자, DN, 유효기간 ...)은 DER 에서 꺼내 컬럼으로도
 * 둔다. 키 식별자와 일련번호는 대문자 16진수다.
 */
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
