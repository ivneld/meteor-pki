package com.meteor.pki.api.request;

import java.time.Instant;
import java.util.List;

import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.usecase.RootIssueCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RootIssuanceRequest(@NotNull Long certProfileId, @NotBlank String subjectDn,
        @Pattern(regexp = "^$|^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod,
        List<String> crlDps, List<String> ocspUris, Instant notBefore) {

    public RootIssueCommand toCommand() {
        return new RootIssueCommand(certProfileId, DistinguishedName.of(subjectDn),
                validityPeriod == null ? null : ValidityDuration.from(validityPeriod), crlDps, ocspUris, notBefore);
    }

}
