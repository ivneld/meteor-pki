package io.autocrypt.vehicles.pki.x509.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RenewalRequestV1(@NotBlank String serialNumberHex, @Min(0) int activationStartDaysAgo,
        boolean revokeOriginalCertificate, byte[] subjectPublicKeyInfoDer, @Pattern(regexp = "^$|^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod) {
}
