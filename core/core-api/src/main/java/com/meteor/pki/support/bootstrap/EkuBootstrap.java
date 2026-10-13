package com.meteor.pki.support.bootstrap;

import com.meteor.pki.domain.EkuWriter;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
