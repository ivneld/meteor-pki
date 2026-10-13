package io.autocrypt.vehicles.pki.x509.api.response;

import io.autocrypt.vehicles.pki.x509.domain.CertProfile;

public record CertProfileResponse(Long id, String name) {

    public static CertProfileResponse from(CertProfile profile) {
        return new CertProfileResponse(profile.getId(), profile.getName());
    }

}
