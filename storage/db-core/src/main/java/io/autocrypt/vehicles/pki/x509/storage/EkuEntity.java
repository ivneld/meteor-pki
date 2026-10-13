package io.autocrypt.vehicles.pki.x509.storage;

import io.autocrypt.vehicles.pki.x509.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 등록된 확장 키 용도(EKU) 영속 모델.
 */
@Entity
@Table(name = "extended_key_usage")
public class EkuEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 128)
    private String oid;

    @Column(nullable = false, length = 128)
    private String purpose;

    protected EkuEntity() {
    }

    public EkuEntity(String oid, String purpose) {
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
