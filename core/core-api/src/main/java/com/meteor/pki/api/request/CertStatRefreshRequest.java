package com.meteor.pki.api.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record CertStatRefreshRequest(@NotNull LocalDate start, @NotNull LocalDate end) {
}
