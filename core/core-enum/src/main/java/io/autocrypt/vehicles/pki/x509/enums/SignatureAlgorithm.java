package io.autocrypt.vehicles.pki.x509.enums;

/**
 * 인증서 서명 알고리즘. JCA 이름과 OID 는 x509-ra 와 같다.
 */
public enum SignatureAlgorithm {

    SHA256_WITH_RSA("SHA256withRSA", "1.2.840.113549.1.1.11"),
    SHA384_WITH_RSA("SHA384withRSA", "1.2.840.113549.1.1.12"),
    SHA256_WITH_ECDSA("SHA256withECDSA", "1.2.840.10045.4.3.2"),
    SHA384_WITH_ECDSA("SHA384withECDSA", "1.2.840.10045.4.3.3"),
    SHA512_WITH_ECDSA("SHA512withECDSA", "1.2.840.10045.4.3.4"), ML_DSA_44("ML-DSA-44", "2.16.840.1.101.3.4.3.17"),
    ED448("Ed448", "1.3.101.113");

    private final String jcaName;

    private final String oid;

    SignatureAlgorithm(String jcaName, String oid) {
        this.jcaName = jcaName;
        this.oid = oid;
    }

    public String getJcaName() {
        return jcaName;
    }

    public String getOid() {
        return oid;
    }

}
