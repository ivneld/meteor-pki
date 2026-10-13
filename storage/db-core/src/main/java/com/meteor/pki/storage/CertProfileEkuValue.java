package com.meteor.pki.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

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
