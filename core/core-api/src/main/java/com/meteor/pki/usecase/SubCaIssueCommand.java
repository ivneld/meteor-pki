package com.meteor.pki.usecase;

import java.time.Instant;
import java.util.List;

import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.domain.IssuanceOptions;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.ValidityDuration;

public record SubCaIssueCommand(Long certProfileId, DistinguishedName subjectDn, KeyIdentifier issuerKeyId,
        List<String> crlDps, boolean includeDefaultCrlDp, List<String> aiaCaIssuers, List<String> ocspUris,
        ValidityDuration validityDuration, Instant notBefore) {

    IssuanceOptions options(Instant now) {
        return new IssuanceOptions(notBefore == null ? now : notBefore, validityDuration, crlDps, includeDefaultCrlDp,
                aiaCaIssuers, ocspUris);
    }

}
