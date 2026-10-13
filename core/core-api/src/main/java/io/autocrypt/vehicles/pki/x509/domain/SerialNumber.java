package io.autocrypt.vehicles.pki.x509.domain;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * 인증서 일련번호. 20 바이트 난수의 첫 바이트를 {@code 01xxxxxx} 로 맞춰 항상 양수이고 길이가 20 바이트다(RFC 5280 4.1.2.2,
 * x509-ra 와 같다). 16진수는 대문자로 쓴다.
 */
public record SerialNumber(BigInteger value) {

    private static final int LENGTH = 20;

    public SerialNumber {
        if (value == null || value.signum() <= 0 || value.bitLength() > LENGTH * 8 - 1) {
            throw new IllegalArgumentException("Serial number must be positive and at most 20 octets.");
        }
    }

    public static SerialNumber random(SecureRandom random) {
        byte[] bytes = new byte[LENGTH];
        random.nextBytes(bytes);
        bytes[0] = (byte) ((bytes[0] & 0x7F) | 0x40);
        return new SerialNumber(new BigInteger(1, bytes));
    }

    /** 대소문자를 가리지 않는다. 형식이 틀리면 IllegalArgumentException(400)이다. */
    public static SerialNumber of(String hex) {
        try {
            return new SerialNumber(new BigInteger(hex == null ? "" : hex.trim(), 16));
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid serial number hex: " + hex);
        }
    }

    public String toHex() {
        return value.toString(16).toUpperCase(Locale.ROOT);
    }

}
