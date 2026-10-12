package com.meteor.domain;

import java.util.regex.Pattern;

/**
 * 이메일 주소. 형식 검증만 한다. "가입 가능한 도메인인가" 같은 정책은 여기 두지 않는다.
 */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid email: " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

}
