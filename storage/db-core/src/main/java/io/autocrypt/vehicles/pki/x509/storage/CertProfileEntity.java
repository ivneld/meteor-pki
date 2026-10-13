package io.autocrypt.vehicles.pki.x509.storage;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyAlgorithmSpec;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;
import io.autocrypt.vehicles.pki.x509.enums.ValidityUnit;
import io.autocrypt.vehicles.pki.x509.support.storage.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * 인증서 프로파일 영속 모델. 도메인을 모르고 core-enum 의 enum 만 안다. 값 객체(유효기간, 인증서 정책, SAN)는 컬럼으로 펼치고, 목록은
 * 컬렉션 테이블에 둔다. 프로파일은 만든 뒤 바뀌지 않으므로 변경 메서드가 없다.
 */
@Entity
@Table(name = "cert_profile")
public class CertProfileEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CertType certType;

    @Column(name = "is_ca", nullable = false)
    private boolean ca;

    private Integer pathLenConstraint;

    @Column(nullable = false)
    private boolean keyUsageCritical;

    @Column(name = "cp_critical", nullable = false)
    private boolean certificatePoliciesCritical;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KeyAlgorithmSpec keySpec;

    @Column(nullable = false, length = 2000)
    private String sanCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ValidityUnit validityUnit;

    @Column(nullable = false)
    private long validityAmount;

    @Column(nullable = false)
    private boolean requireApproval;

    @Column(nullable = false)
    private boolean useDefaultAiaCaIssuer;

    @ElementCollection
    @CollectionTable(name = "cert_profile_key_usage", joinColumns = @JoinColumn(name = "cert_profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "key_usage", nullable = false, length = 30)
    private Set<KeyUsage> keyUsages = EnumSet.noneOf(KeyUsage.class);

    @ElementCollection
    @CollectionTable(name = "cert_profile_extended_key_usage", joinColumns = @JoinColumn(name = "cert_profile_id"))
    @OrderColumn(name = "position")
    private List<CertProfileEkuValue> extendedKeyUsages = new ArrayList<>();

    /** 비어 있으면 인증서 정책 확장이 없다. */
    @ElementCollection
    @CollectionTable(name = "cert_profile_cp_oid", joinColumns = @JoinColumn(name = "cert_profile_id"))
    @OrderColumn(name = "position")
    @Column(name = "cp_oid", nullable = false, length = 128)
    private List<String> policyOids = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "cert_profile_ocsp_uri", joinColumns = @JoinColumn(name = "cert_profile_id"))
    @OrderColumn(name = "position")
    @Column(name = "ocsp_uri", nullable = false, length = 512)
    private List<String> ocspUris = new ArrayList<>();

    protected CertProfileEntity() {
    }

    public CertProfileEntity(String name, CertType certType, boolean ca, Integer pathLenConstraint,
            boolean keyUsageCritical, boolean certificatePoliciesCritical, KeyAlgorithmSpec keySpec, String sanCsv,
            ValidityUnit validityUnit, long validityAmount, boolean requireApproval, boolean useDefaultAiaCaIssuer,
            Set<KeyUsage> keyUsages, List<CertProfileEkuValue> extendedKeyUsages, List<String> policyOids,
            List<String> ocspUris) {
        this.name = name;
        this.certType = certType;
        this.ca = ca;
        this.pathLenConstraint = pathLenConstraint;
        this.keyUsageCritical = keyUsageCritical;
        this.certificatePoliciesCritical = certificatePoliciesCritical;
        this.keySpec = keySpec;
        this.sanCsv = sanCsv;
        this.validityUnit = validityUnit;
        this.validityAmount = validityAmount;
        this.requireApproval = requireApproval;
        this.useDefaultAiaCaIssuer = useDefaultAiaCaIssuer;
        this.keyUsages.addAll(keyUsages);
        this.extendedKeyUsages.addAll(extendedKeyUsages);
        this.policyOids.addAll(policyOids);
        this.ocspUris.addAll(ocspUris);
    }

    public String getName() {
        return name;
    }

    public CertType getCertType() {
        return certType;
    }

    public boolean isCa() {
        return ca;
    }

    public Integer getPathLenConstraint() {
        return pathLenConstraint;
    }

    public boolean isKeyUsageCritical() {
        return keyUsageCritical;
    }

    public boolean isCertificatePoliciesCritical() {
        return certificatePoliciesCritical;
    }

    public KeyAlgorithmSpec getKeySpec() {
        return keySpec;
    }

    public String getSanCsv() {
        return sanCsv;
    }

    public ValidityUnit getValidityUnit() {
        return validityUnit;
    }

    public long getValidityAmount() {
        return validityAmount;
    }

    public boolean isRequireApproval() {
        return requireApproval;
    }

    public boolean isUseDefaultAiaCaIssuer() {
        return useDefaultAiaCaIssuer;
    }

    public Set<KeyUsage> getKeyUsages() {
        return keyUsages;
    }

    public List<CertProfileEkuValue> getExtendedKeyUsages() {
        return extendedKeyUsages;
    }

    public List<String> getPolicyOids() {
        return policyOids;
    }

    public List<String> getOcspUris() {
        return ocspUris;
    }

}
