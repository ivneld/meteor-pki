package io.autocrypt.vehicles.pki.x509.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 발급하는 인증서에 넣는 기본 배포 지점 주소. 기본값은 x509-ra 와 같다(기본 URL {@code http://127.0.0.1}, 경로
 * {@code /api/ocsp}, {@code /api/ca}, {@code /api/crl}).
 */
@ConfigurationProperties("pki.distribution-point")
public record DistributionPointProperties(@DefaultValue("http://127.0.0.1") String ocspBaseUrl,
        @DefaultValue("http://127.0.0.1") String caBaseUrl, @DefaultValue("http://127.0.0.1") String crlBaseUrl,
        @DefaultValue("/api/ocsp") String ocspPath, @DefaultValue("/api/ca") String caPath,
        @DefaultValue("/api/crl") String crlPath) {

}
