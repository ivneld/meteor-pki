package io.autocrypt.vehicles.pki.x509.api.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record P10crIssuanceRequestV1(@NotBlank String p10crPem, @NotNull Long certProfileId,
        @NotBlank String issuerKeyIdHex,
        @Pattern(regexp = "^$|^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod,
        Instant notBefore, List<String> crlDps, List<String> aiaCaIssuers, List<String> ocspUris) {
}
