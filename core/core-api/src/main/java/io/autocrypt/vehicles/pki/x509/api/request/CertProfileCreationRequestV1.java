package io.autocrypt.vehicles.pki.x509.api.request;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * JSON 필드 이름은 extendedKeyUsages 가 아니라 extendKeyUsage 다 (x509-ra 계약).
 */
public record CertProfileCreationRequestV1(@NotBlank String policyName, @NotNull CertType certType,
        @Pattern(regexp = "^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") @NotBlank String validityPeriod,
        Integer pathLenConstraint, String subjectAltName, @NotNull Set<@NotNull KeyUsage> keyUsages,
        @JsonProperty("extendKeyUsage") List<@NotBlank String> extendedKeyUsages, List<@NotBlank String> policyOids,
        Boolean useDefaultOcspUri, List<@NotBlank String> ocspUris, Boolean keyUsageCritical, Boolean requireApproval,
        Boolean useDefaultAiaCaIssuer, @NotNull String keyAlgorithmSpec) {
}
