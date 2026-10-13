package com.meteor.pki.domain;

import java.time.Instant;
import java.util.List;

public record IssuanceOptions(Instant notBefore, ValidityDuration requestedDuration, List<String> crlDps,
        boolean includeDefaultCrlDp, List<String> aiaCaIssuers, List<String> ocspUris) {

    public IssuanceOptions {
        crlDps = crlDps == null ? List.of() : List.copyOf(crlDps);
        aiaCaIssuers = aiaCaIssuers == null ? List.of() : List.copyOf(aiaCaIssuers);
        ocspUris = ocspUris == null ? List.of() : List.copyOf(ocspUris);
    }

    ValidityPeriod validityWithin(ValidityDuration profileDuration) {
        Instant requestedNotAfter = requestedDuration == null ? null : requestedDuration.maxNotAfter(notBefore);
        return profileDuration.resolve(notBefore, requestedNotAfter);
    }

}
