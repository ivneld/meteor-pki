package io.autocrypt.vehicles.pki.x509.api.response;

public record RenewalResponse(String serialNumberHex, String subjectDn, String certificateDerBase64) {
}
