package com.meteor.pki.api.request;

import java.time.Instant;
import java.util.List;

import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SubjectPublicKey;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.usecase.LeafIssueCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LeafIssuanceRequest(@NotBlank String subjectDn, @NotBlank String akiHex,
        @NotNull byte[] subjectPublicKeyInfoDer, @NotNull Long certProfileId, List<String> crlDps,
        boolean includeDefaultCrlDp, List<String> aiaCaIssuers, List<String> ocspUris,
        @Pattern(regexp = "^$|^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod,
        Instant notBefore) {

    public LeafIssueCommand toCommand() {
        return new LeafIssueCommand(certProfileId, DistinguishedName.of(subjectDn), KeyIdentifier.of(akiHex),
                new SubjectPublicKey(subjectPublicKeyInfoDer), crlDps, includeDefaultCrlDp, aiaCaIssuers, ocspUris,
                validityPeriod == null ? null : ValidityDuration.from(validityPeriod), notBefore);
    }

}
