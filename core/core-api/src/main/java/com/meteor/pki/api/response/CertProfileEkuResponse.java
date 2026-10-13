package com.meteor.pki.api.response;

import com.meteor.pki.domain.ProfileEku;

public record CertProfileEkuResponse(String oid, String purpose) {

    public static CertProfileEkuResponse from(ProfileEku eku) {
        return new CertProfileEkuResponse(eku.oid(), eku.purpose());
    }

}
