package io.autocrypt.vehicles.pki.x509.api.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * notBefore 가 비어 있으면 현재 시각.
 */
public record SubCaIssuanceRequestV1(@NotNull Long certProfileId, @NotBlank String subjectDn, @NotBlank String akiHex,
        List<String> crlDps, List<String> aiaCaIssuers, List<String> ocspUris,
        @Pattern(regexp = "^$|^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod,
        Instant notBefore) {
}
