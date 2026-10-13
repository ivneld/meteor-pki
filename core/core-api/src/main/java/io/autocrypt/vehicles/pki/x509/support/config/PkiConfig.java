package io.autocrypt.vehicles.pki.x509.support.config;

import java.time.Clock;

import io.autocrypt.vehicles.pki.x509.domain.DistributionPoints;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 발급 흐름이 쓰는 설정 값을 도메인 값 객체로 바꿔 빈으로 둔다.
 */
@Configuration
public class PkiConfig {

    @Bean
    DistributionPoints distributionPoints(DistributionPointProperties properties) {
        return new DistributionPoints(properties.ocspBaseUrl(), properties.caBaseUrl(), properties.crlBaseUrl(),
                properties.ocspPath(), properties.caPath(), properties.crlPath());
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

}
