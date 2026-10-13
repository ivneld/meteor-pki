package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;
import java.util.regex.Pattern;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

/**
 * 인증서 정책(certificatePolicies 확장)에 넣을 정책 OID 목록. 확장을 넣기로 했다면 OID 가 하나 이상이어야 한다(RFC 5280
 * 4.2.1.4).
 */
public record CertificatePolicies(List<String> oids, boolean critical) {

    private static final Pattern OID = Pattern.compile("\\d+(\\.\\d+)+");

    public CertificatePolicies {
        if (oids == null || oids.isEmpty()) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "certificatePolicies must contain at least one policy OID (RFC §4.2.1.4).");
        }
        for (String oid : oids) {
            if (oid == null || !OID.matcher(oid).matches()) {
                throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid OID.");
            }
        }
        oids = List.copyOf(oids);
    }

    /** API 로 만드는 정책은 non-critical 이다. 목록이 없으면(null) 확장을 넣지 않는다. */
    public static CertificatePolicies ofNullable(List<String> oids) {
        return oids == null ? null : new CertificatePolicies(oids, false);
    }

}
