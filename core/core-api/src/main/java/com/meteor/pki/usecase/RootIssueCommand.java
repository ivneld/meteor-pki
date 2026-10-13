package com.meteor.pki.usecase;

import java.time.Instant;
import java.util.List;

import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.domain.IssuanceOptions;
import com.meteor.pki.domain.ValidityDuration;

public record RootIssueCommand(Long certProfileId, DistinguishedName subjectDn, ValidityDuration validityDuration,
        List<String> crlDps, List<String> ocspUris, Instant notBefore) {

    IssuanceOptions options(Instant now) {
        return new IssuanceOptions(notBefore == null ? now : notBefore, validityDuration, crlDps, false, List.of(),
                ocspUris);
    }

}
