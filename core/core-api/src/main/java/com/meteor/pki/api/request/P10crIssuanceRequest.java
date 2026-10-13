package com.meteor.pki.api.request;

import java.time.Instant;
import java.util.List;

import com.meteor.pki.domain.CertificationRequest;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.usecase.LeafP10crIssueCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record P10crIssuanceRequest(@NotBlank String p10crPem, @NotNull Long certProfileId, @NotBlank String akiHex,
        List<String> crlDps, boolean includeDefaultCrlDp, List<String> aiaCaIssuers, List<String> ocspUris,
        @Pattern(regexp = "^$|^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod,
        Instant notBefore) {

    public LeafP10crIssueCommand toCommand() {
        return new LeafP10crIssueCommand(certProfileId, CertificationRequest.fromPem(p10crPem),
                KeyIdentifier.of(akiHex), crlDps, includeDefaultCrlDp, aiaCaIssuers, ocspUris,
                validityPeriod == null ? null : ValidityDuration.from(validityPeriod), notBefore);
    }

}
