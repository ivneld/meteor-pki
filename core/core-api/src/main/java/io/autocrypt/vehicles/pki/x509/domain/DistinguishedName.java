package io.autocrypt.vehicles.pki.x509.domain;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;

/**
 * 인증서의 주체·발급자 이름. RFC 4514 문자열(예: {@code CN=Root CA,O=Autocrypt,C=KR})이며 BouncyCastle 표기로
 * 맞춰 둔다.
 */
public record DistinguishedName(String value) {

    public DistinguishedName {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "DistinguishedName must not be blank.");
        }
    }

    /** 문법이 틀리면 IllegalArgumentException(400)이다. */
    public static DistinguishedName of(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "DistinguishedName must not be blank.");
        }
        return from(new X500Name(BCStyle.INSTANCE, value));
    }

    public static DistinguishedName from(X500Name name) {
        return new DistinguishedName(BCStyle.INSTANCE.toString(name));
    }

    public X500Name toX500Name() {
        return new X500Name(BCStyle.INSTANCE, value);
    }

}
