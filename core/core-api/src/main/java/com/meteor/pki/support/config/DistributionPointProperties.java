package com.meteor.pki.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("pki.distribution-point")
public record DistributionPointProperties(@DefaultValue("http://127.0.0.1") String ocspBaseUrl,
        @DefaultValue("http://127.0.0.1") String caBaseUrl, @DefaultValue("http://127.0.0.1") String crlBaseUrl,
        @DefaultValue("/api/ocsp") String ocspPath, @DefaultValue("/api/ca") String caPath,
        @DefaultValue("/api/crl") String crlPath) {

}
