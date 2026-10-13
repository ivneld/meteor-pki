package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;

/**
 * 등록된 확장 키 용도(EKU). 프로파일은 여기 등록된 OID 만 쓸 수 있다. 상태 전이가 없는 코드 관리 기능이라 단순한 모델이다(S-02).
 */
public class Eku {

    private static final int MAX_LENGTH = 128;

    private final Long id;

    private final String oid;

    private final String purpose;

    private Eku(Long id, String oid, String purpose) {
        this.id = id;
        this.oid = oid;
        this.purpose = purpose;
    }

    /** OID 형식이 틀리면 IllegalArgumentException(400)이다. */
    public static Eku register(String oid, String purpose) {
        new ASN1ObjectIdentifier(oid);
        if (oid.length() > MAX_LENGTH) {
            throw new CoreException(ErrorCode.PROFILE_INVALID_INPUT, "OID must be at most 128 characters.");
        }
        if (purpose == null || purpose.isBlank() || purpose.length() > MAX_LENGTH) {
            throw new CoreException(ErrorCode.PROFILE_INVALID_INPUT,
                    "Purpose must not be blank and must be at most 128 characters.");
        }
        return new Eku(null, oid, purpose);
    }

    public static Eku restore(Long id, String oid, String purpose) {
        return new Eku(id, oid, purpose);
    }

    /** 처음 기동할 때 등록해 두는 RFC 5280 4.2.1.12 의 표준 EKU. */
    public static List<Eku> defaults() {
        return List.of(register("1.3.6.1.5.5.7.3.1", "TLS Web Server Authentication"),
                register("1.3.6.1.5.5.7.3.2", "TLS Web Client Authentication"),
                register("1.3.6.1.5.5.7.3.3", "Code Signing"), register("1.3.6.1.5.5.7.3.4", "E-mail Protection"),
                register("1.3.6.1.5.5.7.3.8", "Time Stamping"), register("1.3.6.1.5.5.7.3.9", "OCSP Signing"));
    }

    /** 프로파일에 복사해 넣을 값. */
    public ProfileEku toProfileEku() {
        return new ProfileEku(oid, purpose);
    }

    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public String getOid() {
        return oid;
    }

    public String getPurpose() {
        return purpose;
    }

}
