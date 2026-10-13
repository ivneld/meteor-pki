package io.autocrypt.vehicles.pki.x509.usecase;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.domain.DistinguishedName;
import io.autocrypt.vehicles.pki.x509.domain.IssuanceOptions;
import io.autocrypt.vehicles.pki.x509.domain.ValidityDuration;

/**
 * @param validityDuration 없으면(null) 프로파일의 최대 기간
 * @param notBefore 없으면(null) 현재 시각
 */
public record RootIssueCommand(Long certProfileId, DistinguishedName subjectDn, ValidityDuration validityDuration,
        List<String> crlDps, List<String> ocspUris, Instant notBefore) {

    IssuanceOptions options(Instant now) {
        return new IssuanceOptions(notBefore == null ? now : notBefore, validityDuration, crlDps, false, List.of(),
                ocspUris);
    }

}
