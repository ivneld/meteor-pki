package io.autocrypt.vehicles.pki.x509.api.response;

import io.autocrypt.vehicles.pki.x509.domain.Eku;

public record EkuResponse(Long id, String oid, String purpose) {

    public static EkuResponse from(Eku eku) {
        return new EkuResponse(eku.getId(), eku.getOid(), eku.getPurpose());
    }

}
