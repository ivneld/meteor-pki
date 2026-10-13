package io.autocrypt.vehicles.pki.x509.api.request;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.domain.DistinguishedName;
import io.autocrypt.vehicles.pki.x509.domain.ValidityDuration;
import io.autocrypt.vehicles.pki.x509.usecase.RootIssueCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * notBefore 가 비어 있으면 현재 시각.
 */
public record RootIssuanceRequest(@NotNull Long certProfileId, @NotBlank String subjectDn,
        @Pattern(regexp = "^$|^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod,
        List<String> crlDps, List<String> ocspUris, Instant notBefore) {

    public RootIssueCommand toCommand() {
        return new RootIssueCommand(certProfileId, DistinguishedName.of(subjectDn),
                validityPeriod == null ? null : ValidityDuration.from(validityPeriod), crlDps, ocspUris, notBefore);
    }

}
