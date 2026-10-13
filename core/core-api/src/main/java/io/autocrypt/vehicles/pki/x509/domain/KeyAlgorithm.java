package io.autocrypt.vehicles.pki.x509.domain;

import java.util.Arrays;

import io.autocrypt.vehicles.pki.x509.enums.KeyAlgorithmSpec;
import io.autocrypt.vehicles.pki.x509.enums.SignatureAlgorithm;
import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

/**
 * 키 알고리즘과 크기. API 의 표시값({@code EC-P-256})과 enum 사이를 바꾸고, 키로 서명할 때의 알고리즘을 알려 준다.
 */
public record KeyAlgorithm(KeyAlgorithmSpec spec) {

    public KeyAlgorithm {
        if (spec == null) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Unsupported key algorithm spec.");
        }
    }

    /** API 표시값(예: {@code RSA-2048}, {@code EC-P-256})을 정확히 맞춰 읽는다. */
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
