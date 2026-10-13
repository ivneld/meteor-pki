package com.meteor.pki.domain;

import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;

public record DistributionPoints(String ocspBaseUrl, String caBaseUrl, String crlBaseUrl, String ocspPath,
        String caPath, String crlPath) {

    public DistributionPoints {
        ocspBaseUrl = requireBaseUrl(ocspBaseUrl);
        caBaseUrl = requireBaseUrl(caBaseUrl);
        crlBaseUrl = requireBaseUrl(crlBaseUrl);
    }

    public String defaultOcspUri() {
        return join(ocspBaseUrl, ocspPath);
    }

    public String caIssuersUrl(KeyIdentifier issuerKeyId) {
        return join(caBaseUrl, caPath) + "/" + issuerKeyId.toHex() + ".crt";
    }

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
