package com.meteor.pki.api.request;

import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SerialNumber;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.usecase.RootRenewCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RootRenewalRequest(@NotBlank String serialNumberHex, @NotBlank String akiHex,
        @Min(0) int activationStartDaysAgo, @Pattern(regexp = "^$|^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod) {

    public RootRenewCommand toCommand() {
        return new RootRenewCommand(SerialNumber.of(serialNumberHex), KeyIdentifier.of(akiHex), activationStartDaysAgo,
                validityPeriod == null || validityPeriod.isEmpty() ? null : ValidityDuration.from(validityPeriod));
    }

}
