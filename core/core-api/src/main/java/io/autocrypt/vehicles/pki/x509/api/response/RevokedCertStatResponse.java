package io.autocrypt.vehicles.pki.x509.api.response;

public record RevokedCertStatResponse(String groupBy, Long revokedCertCount) {
}
