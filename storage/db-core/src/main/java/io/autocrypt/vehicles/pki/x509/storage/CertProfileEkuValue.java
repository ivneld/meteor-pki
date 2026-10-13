package io.autocrypt.vehicles.pki.x509.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 프로파일에 복사해 둔 EKU 한 건(OID, 설명).
 */
@Embeddable
public class CertProfileEkuValue {

    @Column(nullable = false, length = 128)
    private String oid;

    @Column(nullable = false, length = 128)
    private String purpose;

    protected CertProfileEkuValue() {
    }

    public CertProfileEkuValue(String oid, String purpose) {
        this.oid = oid;
        this.purpose = purpose;
    }

    public String getOid() {
        return oid;
    }

    public String getPurpose() {
        return purpose;
    }

}
