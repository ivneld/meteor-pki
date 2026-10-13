package com.meteor.pki.api.request;

import jakarta.validation.constraints.NotBlank;

public record EkuCreateRequest(@NotBlank String oid, @NotBlank String purpose) {
}
