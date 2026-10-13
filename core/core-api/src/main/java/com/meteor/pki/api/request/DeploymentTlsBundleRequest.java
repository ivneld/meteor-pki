package com.meteor.pki.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeploymentTlsBundleRequest(@NotBlank @Size(max = 2000) String subjectAlternativeNames,
        @NotBlank String pfxPassword) {
}
