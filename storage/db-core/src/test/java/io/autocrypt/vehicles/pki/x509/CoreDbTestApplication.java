package io.autocrypt.vehicles.pki.x509;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 이 모듈만 단독으로 컨텍스트 테스트할 때 사용하는 진입점. 저장소 인프라 설정(support.storage)을 스캔한다.
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class CoreDbTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreDbTestApplication.class, args);
    }

}
