package io.autocrypt.vehicles.pki.x509.support.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * storage.datasource.core.* 프로퍼티로 Hikari 풀을 직접 구성한다. 저장소가 늘어나면 같은 패턴으로 모듈을 추가한다.
 */
@Configuration
class CoreDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "storage.datasource.core")
    HikariConfig coreHikariConfig() {
        return new HikariConfig();
    }

    @Bean
    HikariDataSource coreDataSource(@Qualifier("coreHikariConfig") HikariConfig config) {
        return new HikariDataSource(config);
    }

}
