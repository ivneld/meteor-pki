package com.meteor.pki.api.request;

import jakarta.validation.constraints.NotBlank;

public record CertProfileCustomExtensionCreationRequest(@NotBlank(message = "Custom OID is required.") String oid,
        boolean critical, String oidValue) {
}
