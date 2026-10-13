package io.autocrypt.vehicles.pki.x509.api.response;

import io.autocrypt.vehicles.pki.x509.domain.ProfileEku;

public record CertProfileEkuResponse(String oid, String purpose) {

    public static CertProfileEkuResponse from(ProfileEku eku) {
        return new CertProfileEkuResponse(eku.oid(), eku.purpose());
    }

}
