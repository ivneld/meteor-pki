package com.meteor.pki.domain;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Locale;

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
