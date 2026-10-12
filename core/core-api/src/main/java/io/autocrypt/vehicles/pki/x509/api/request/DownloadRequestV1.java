package io.autocrypt.vehicles.pki.x509.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 인증서 다운로드 쿼리 파라미터. format 은 pem(기본) 또는 der.
 */
public record DownloadRequestV1(@NotBlank String serialNumberHex, String format) {
}
