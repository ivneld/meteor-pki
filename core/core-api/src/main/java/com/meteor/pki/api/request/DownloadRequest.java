package com.meteor.pki.api.request;

import jakarta.validation.constraints.NotBlank;

public record DownloadRequest(@NotBlank String serialNumberHex, String format) {
}
