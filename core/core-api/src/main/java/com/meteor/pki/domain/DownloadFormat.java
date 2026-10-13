package com.meteor.pki.domain;

import java.util.Locale;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

public record DownloadFormat(boolean der) {

    public static DownloadFormat of(String format) {
        if (format == null || format.isBlank()) {
            return new DownloadFormat(false);
        }
        return switch (format.trim().toLowerCase(Locale.ROOT)) {
            case "pem" -> new DownloadFormat(false);
            case "der" -> new DownloadFormat(true);
            default -> throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Unsupported certificate download format.")
                .property("format", format);
        };
    }

    public String extension() {
        return der ? "der" : "pem";
    }

}
