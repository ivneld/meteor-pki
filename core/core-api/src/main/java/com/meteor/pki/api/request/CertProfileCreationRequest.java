package com.meteor.pki.api.request;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.meteor.pki.domain.CertificatePolicies;
import com.meteor.pki.domain.KeyAlgorithm;
import com.meteor.pki.domain.SubjectAltNameTemplate;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;
import com.meteor.pki.usecase.CertProfileCreateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

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
