package com.meteor.pki.domain;

import java.util.Arrays;

import com.meteor.pki.enums.KeyAlgorithmSpec;
import com.meteor.pki.enums.SignatureAlgorithm;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

public record KeyAlgorithm(KeyAlgorithmSpec spec) {

    public KeyAlgorithm {
        if (spec == null) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Unsupported key algorithm spec.");
        }
    }

    public static KeyAlgorithm of(String value) {
        return Arrays.stream(KeyAlgorithmSpec.values())
            .filter(spec -> spec.getValue().equals(value))
            .findFirst()
            .map(KeyAlgorithm::new)
            .orElseThrow(() -> new CoreException(ErrorCode.CERT_INVALID_INPUT, "Unsupported key algorithm spec."));
    }

    public String value() {
        return spec.getValue();
    }

    public SignatureAlgorithm signatureAlgorithm() {
        return spec.getSignatureAlgorithm();
    }

}
