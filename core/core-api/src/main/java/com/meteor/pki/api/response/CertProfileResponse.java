package com.meteor.pki.api.response;

import com.meteor.pki.domain.CertProfile;

public record CertProfileResponse(Long id, String name) {

    public static CertProfileResponse from(CertProfile profile) {
        return new CertProfileResponse(profile.getId(), profile.getName());
    }

}
