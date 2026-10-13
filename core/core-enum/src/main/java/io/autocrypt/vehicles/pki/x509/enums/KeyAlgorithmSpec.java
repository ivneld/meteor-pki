package io.autocrypt.vehicles.pki.x509.enums;

/**
 * 키 알고리즘과 크기. API 에서는 표시값({@link #getValue()}, 예: {@code EC-P-256})으로 주고받고, 저장소에는 enum
 * 이름으로 둔다. 키로 서명할 때 쓰는 알고리즘은 {@link #getSignatureAlgorithm()} 이다.
 */
public enum KeyAlgorithmSpec {

    RSA_2048("RSA-2048", "1.2.840.113549.1.1.1", null, SignatureAlgorithm.SHA256_WITH_RSA),
    RSA_3072("RSA-3072", "1.2.840.113549.1.1.1", null, SignatureAlgorithm.SHA384_WITH_RSA),
    RSA_4096("RSA-4096", "1.2.840.113549.1.1.1", null, SignatureAlgorithm.SHA384_WITH_RSA),
    EC_P256("EC-P-256", "1.2.840.10045.2.1", "1.2.840.10045.3.1.7", SignatureAlgorithm.SHA256_WITH_ECDSA),
    EC_P384("EC-P-384", "1.2.840.10045.2.1", "1.3.132.0.34", SignatureAlgorithm.SHA384_WITH_ECDSA),
    EC_P521("EC-P-521", "1.2.840.10045.2.1", "1.3.132.0.35", SignatureAlgorithm.SHA512_WITH_ECDSA),
    ML_DSA_44("ML-DSA-44", "2.16.840.1.101.3.4.3.17", null, SignatureAlgorithm.ML_DSA_44),
    ED448("Ed448", "1.3.101.113", null, SignatureAlgorithm.ED448);

    private final String value;

    private final String algorithmOid;

    private final String curveOid;

    private final SignatureAlgorithm signatureAlgorithm;

    KeyAlgorithmSpec(String value, String algorithmOid, String curveOid, SignatureAlgorithm signatureAlgorithm) {
        this.value = value;
        this.algorithmOid = algorithmOid;
        this.curveOid = curveOid;
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getValue() {
        return value;
    }

    public String getAlgorithmOid() {
        return algorithmOid;
    }

    /** EC 키의 곡선 OID. EC 가 아니면 null. */
    public String getCurveOid() {
        return curveOid;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

}
