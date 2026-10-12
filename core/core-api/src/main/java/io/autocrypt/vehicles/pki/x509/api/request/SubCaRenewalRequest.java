package io.autocrypt.vehicles.pki.x509.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SubCaRenewalRequest(@NotBlank String serialNumberHex, @NotBlank String akiHex,
        @Min(0) int activationStartDaysAgo,
        @Pattern(regexp = "^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod,
        @NotNull Boolean useExistingPublicKey, @NotNull Boolean revokeOriginalCertificate,
        @NotNull Boolean useLatestIssuer) {
}
