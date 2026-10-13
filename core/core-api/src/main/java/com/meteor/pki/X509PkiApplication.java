package com.meteor.pki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class X509PkiApplication {

    public static void main(String[] args) {
        SpringApplication.run(X509PkiApplication.class, args);
    }

}
