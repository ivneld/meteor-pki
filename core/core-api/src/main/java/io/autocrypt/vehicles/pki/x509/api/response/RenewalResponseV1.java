package io.autocrypt.vehicles.pki.x509.api.response;

public record RenewalResponseV1(String serialNumberHex, String subjectDn, String certificateDerBase64) {
}
