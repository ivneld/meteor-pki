package com.meteor.pki.api.request;

import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SerialNumber;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.usecase.SubCaRenewCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SubCaRenewalRequest(@NotBlank String serialNumberHex, @NotBlank String akiHex,
        @Min(0) int activationStartDaysAgo,
        @Pattern(regexp = "^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod,
        @NotNull Boolean revokeOriginalCertificate, @NotNull Boolean useLatestIssuer) {

    public SubCaRenewCommand toCommand() {
        return new SubCaRenewCommand(SerialNumber.of(serialNumberHex), KeyIdentifier.of(akiHex), activationStartDaysAgo,
                validityPeriod == null ? null : ValidityDuration.from(validityPeriod), revokeOriginalCertificate,
                useLatestIssuer);
    }

}
