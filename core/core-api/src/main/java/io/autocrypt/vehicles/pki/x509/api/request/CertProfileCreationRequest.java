package io.autocrypt.vehicles.pki.x509.api.request;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.autocrypt.vehicles.pki.x509.domain.CertificatePolicies;
import io.autocrypt.vehicles.pki.x509.domain.KeyAlgorithm;
import io.autocrypt.vehicles.pki.x509.domain.SubjectAltNameTemplate;
import io.autocrypt.vehicles.pki.x509.domain.ValidityDuration;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;
import io.autocrypt.vehicles.pki.x509.usecase.CertProfileCreateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * JSON 필드 이름은 extendedKeyUsages 가 아니라 extendKeyUsage 다 (x509-ra 계약).
 */
public record CertProfileCreationRequest(@NotBlank String policyName, @NotNull CertType certType,
        @Pattern(regexp = "^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") @NotBlank String validityPeriod,
        Integer pathLenConstraint, String subjectAltName, @NotNull Set<@NotNull KeyUsage> keyUsages,
        @JsonProperty("extendKeyUsage") List<@NotBlank String> extendedKeyUsages, List<@NotBlank String> policyOids,
        Boolean useDefaultOcspUri, List<@NotBlank String> ocspUris, Boolean keyUsageCritical, Boolean requireApproval,
        Boolean useDefaultAiaCaIssuer, @NotNull String keyAlgorithmSpec,
        List<@Valid CertProfileCustomExtensionCreationRequest> profileCustomExtensions) {

    public CertProfileCreationRequest {
        profileCustomExtensions = Optional.ofNullable(profileCustomExtensions).orElse(List.of());
    }

    /** 빠진 값은 x509-ra 의 기본값으로 채운다. useDefaultOcspUri 만 기본이 true 다. */
    public CertProfileCreateCommand toCommand() {
        List<String> ekuOids = Optional.ofNullable(extendedKeyUsages)
            .orElse(List.of())
            .stream()
            .filter(oid -> oid != null && !oid.isBlank())
            .map(String::trim)
            .toList();
        return new CertProfileCreateCommand(policyName, certType, ValidityDuration.from(validityPeriod),
                pathLenConstraint, SubjectAltNameTemplate.of(subjectAltName), keyUsages,
                Boolean.TRUE.equals(keyUsageCritical), ekuOids, CertificatePolicies.ofNullable(policyOids),
                Optional.ofNullable(ocspUris).orElse(List.of()), !Boolean.FALSE.equals(useDefaultOcspUri),
                Boolean.TRUE.equals(requireApproval), Boolean.TRUE.equals(useDefaultAiaCaIssuer),
                KeyAlgorithm.of(keyAlgorithmSpec),
                profileCustomExtensions.stream()
                    .filter(extension -> extension != null)
                    .map(CertProfileCustomExtensionCreationRequest::oid)
                    .toList());
    }

}
