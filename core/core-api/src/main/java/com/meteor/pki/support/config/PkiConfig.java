package com.meteor.pki.support.config;

import java.time.Clock;

import com.meteor.pki.domain.DistributionPoints;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
