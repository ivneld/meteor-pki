package com.meteor.pki.api.response;

import com.meteor.pki.domain.Eku;

public record EkuResponse(Long id, String oid, String purpose) {

    public static EkuResponse from(Eku eku) {
        return new EkuResponse(eku.getId(), eku.getOid(), eku.getPurpose());
    }

}
