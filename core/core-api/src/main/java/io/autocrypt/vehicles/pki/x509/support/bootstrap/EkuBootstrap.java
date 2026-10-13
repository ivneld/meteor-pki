package io.autocrypt.vehicles.pki.x509.support.bootstrap;

import io.autocrypt.vehicles.pki.x509.domain.EkuWriter;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 기동할 때 표준 EKU 를 등록해 둔다. 이미 있는 것은 건너뛴다. {@code pki.bootstrap.eku.enabled=false} 로 끈다.
 */
@Component
@ConditionalOnProperty(name = "pki.bootstrap.eku.enabled", havingValue = "true", matchIfMissing = true)
public class EkuBootstrap implements ApplicationRunner {

    private final EkuWriter ekuWriter;

    public EkuBootstrap(EkuWriter ekuWriter) {
        this.ekuWriter = ekuWriter;
    }

    @Override
    public void run(ApplicationArguments args) {
        ekuWriter.registerDefaults();
    }

}
