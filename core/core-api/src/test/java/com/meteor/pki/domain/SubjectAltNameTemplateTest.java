package com.meteor.pki.domain;

import com.meteor.pki.support.error.CoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectAltNameTemplateTest {

    @Test
    void emptyMeansNoSan() {
        assertThat(SubjectAltNameTemplate.of(null).isEmpty()).isTrue();
        assertThat(SubjectAltNameTemplate.of("  ").csv()).isEmpty();
    }

    @Test
    void normalizesCommaSeparatedEntries() {
        SubjectAltNameTemplate template = SubjectAltNameTemplate
            .of(" DNS:ra.example.com , ip:10.0.0.1,EMAIL:ops@example.com,URI:https://example.com/a ,");

        assertThat(template.csv())
            .isEqualTo("DNS:ra.example.com,ip:10.0.0.1,EMAIL:ops@example.com,URI:https://example.com/a");
        assertThat(template.entries()).extracting(SubjectAltNameTemplate.Entry::type)
            .containsExactly("DNS", "IP", "EMAIL", "URI");
        assertThat(template.toGeneralNames().getNames()).hasSize(4);
    }

    @Test
    void acceptsWildcardAndIpv6() {
        assertThat(SubjectAltNameTemplate.of("DNS:*.example.com;IP:2001:db8::1").entries()).hasSize(2);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|',
            value = { "example.com|Each SubjectAltName must be TYPE:VALUE", "DNS: |SubjectAltName value is empty.",
                    "X400:abc|Unsupported SubjectAltName type: X400",
                    "DNS:-bad.example.com|Invalid DNS name: -bad.example.com",
                    "IP:300.1.1.1|Invalid IP address: 300.1.1.1", "EMAIL:nobody|Invalid email address: nobody" })
    void rejectsInvalidEntries(String text, String message) {
        assertThatThrownBy(() -> SubjectAltNameTemplate.of(text)).isInstanceOf(CoreException.class).hasMessage(message);
    }

    @Test
    void separatorsOnlyHaveNoEntry() {
        assertThatThrownBy(() -> SubjectAltNameTemplate.of(";,;"))
            .hasMessage("SubjectAltName requires at least one entry");
    }

}
