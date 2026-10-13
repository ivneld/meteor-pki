package io.autocrypt.vehicles.pki.x509.domain;

import java.util.Locale;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

/**
 * 인증서 다운로드 형식. pem(기본) 또는 der. 대소문자를 가리지 않는다.
 */
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
