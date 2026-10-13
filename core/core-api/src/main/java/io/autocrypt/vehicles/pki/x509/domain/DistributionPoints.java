package io.autocrypt.vehicles.pki.x509.domain;

import io.autocrypt.vehicles.pki.x509.support.error.CoreException;
import io.autocrypt.vehicles.pki.x509.support.error.ErrorCode;

/**
 * 발급하는 인증서에 넣는 기본 배포 지점(OCSP, CA 인증서, CRL)의 주소. 기본 URL 과 경로를 합쳐 만든다.
 */
public record DistributionPoints(String ocspBaseUrl, String caBaseUrl, String crlBaseUrl, String ocspPath,
        String caPath, String crlPath) {

    public DistributionPoints {
        ocspBaseUrl = requireBaseUrl(ocspBaseUrl);
        caBaseUrl = requireBaseUrl(caBaseUrl);
        crlBaseUrl = requireBaseUrl(crlBaseUrl);
    }

    /** 프로파일을 만들 때 기본으로 넣는 OCSP 주소. */
    public String defaultOcspUri() {
        return join(ocspBaseUrl, ocspPath);
    }

    /** 발급자 CA 인증서를 받는 주소(AIA caIssuers). 발급자 SKI 로 정해진다. */
    public String caIssuersUrl(KeyIdentifier issuerKeyId) {
        return join(caBaseUrl, caPath) + "/" + issuerKeyId.toHex() + ".crt";
    }

    /** 발급자가 낸 CRL 을 받는 주소(CRL DP). 발급자 SKI 로 정해진다. */
    public String crlUrl(KeyIdentifier issuerKeyId) {
        return join(crlBaseUrl, crlPath) + "/" + issuerKeyId.toHex() + ".crl";
    }

    private static String join(String base, String path) {
        String trimmedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path == null || path.isBlank() ? "" : (path.startsWith("/") ? path : "/" + path);
        return trimmedBase + normalizedPath;
    }

    private static String requireBaseUrl(String url) {
        String trimmed = url == null ? "" : url.trim();
        if (trimmed.isEmpty() || trimmed.length() > 500
                || !(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT,
                    "Distribution point base URL must start with http:// or https://");
        }
        return trimmed;
    }

}
