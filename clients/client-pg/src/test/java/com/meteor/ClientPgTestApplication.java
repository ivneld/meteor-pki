package com.meteor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class ClientPgTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientPgTestApplication.class, args);
    }

}
