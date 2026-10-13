package com.meteor.pki.support.web;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer x509RaCompatibleCustomizer() {
        return JacksonConfig::customize;
    }

    public static void customize(JsonMapper.Builder builder) {
        SimpleModule module = new SimpleModule("instant-in-seconds");
        module.addSerializer(Instant.class, new ValueSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(value.truncatedTo(ChronoUnit.SECONDS).toString());
            }
        });
        builder.addModule(module).disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    }

}
