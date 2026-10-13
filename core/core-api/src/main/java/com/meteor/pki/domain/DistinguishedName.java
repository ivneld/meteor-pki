package com.meteor.pki.domain;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;

public record DistinguishedName(String value) {

    public DistinguishedName {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "DistinguishedName must not be blank.");
        }
    }

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
