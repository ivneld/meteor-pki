package com.meteor.pki.domain;

import java.util.List;
import java.util.regex.Pattern;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

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

    public static CertificatePolicies ofNullable(List<String> oids) {
        return oids == null ? null : new CertificatePolicies(oids, false);
    }

}
