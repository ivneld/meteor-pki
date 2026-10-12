package io.autocrypt.vehicles.pki.x509.support.web;

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

/**
 * x509-ra(Jackson 2)와 같은 JSON 계약을 맞춘다.
 *
 * <ul>
 * <li>{@link Instant} 는 초 단위로 잘라 ISO-8601 문자열로 쓴다 ({@code 2026-01-01T00:00:00Z}).</li>
 * <li>요청에 없는 primitive 필드(boolean, int)는 기본값(false, 0)으로 받는다. Jackson 3 은 기본으로 거절하지만
 * x509-ra 는 받았다.</li>
 * <li>byte[] 는 Jackson 기본대로 Base64 문자열이다.</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer x509RaCompatibleCustomizer() {
        return JacksonConfig::customize;
    }

    /** 웹 슬라이스 테스트가 같은 설정의 매퍼를 만들 수 있게 공개한다. */
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
