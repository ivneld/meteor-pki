package com.meteor.pki.ui.form;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import com.meteor.pki.domain.SubjectPublicKey;
import com.meteor.pki.domain.ValidityDuration;

final class FormValues {

    private FormValues() {
    }

    static List<String> lines(String text) {
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(text.split("[\\r\\n,]+")).map(String::trim).filter(line -> !line.isEmpty()).toList();
    }

    static ValidityDuration duration(String text) {
        return blank(text) ? null : ValidityDuration.from(text.trim());
    }

    static Instant instant(String localDateTime) {
        if (blank(localDateTime)) {
            return null;
        }
        try {
            return LocalDateTime.parse(localDateTime.trim()).atZone(ZoneId.systemDefault()).toInstant();
        }
        catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("시작 시각 형식이 올바르지 않습니다.");
        }
    }

    static SubjectPublicKey publicKey(String pem) {
        if (blank(pem)) {
            return null;
        }
        String body = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        try {
            return new SubjectPublicKey(Base64.getDecoder().decode(body));
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("공개키는 PEM(-----BEGIN PUBLIC KEY-----) 이나 Base64 SPKI DER 이어야 합니다.");
        }
    }

    static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    static boolean checked(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

}
