package io.autocrypt.vehicles.pki.x509.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * 키 식별자(SKI, AKI). 공개키 BIT STRING 의 SHA-1 이다(RFC 5280 4.2.1.2 방법 1). 16진수는 대문자로 쓴다.
 */
public record KeyIdentifier(byte[] value) {

    public KeyIdentifier {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("Key identifier must not be empty.");
        }
        value = value.clone();
    }

    public static KeyIdentifier of(String hex) {
        try {
            return new KeyIdentifier(HexFormat.of().parseHex(hex == null ? "" : hex.trim()));
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid key identifier hex: " + hex);
        }
    }

    public static KeyIdentifier fromSpki(SubjectPublicKeyInfo spki) {
        try {
            return new KeyIdentifier(MessageDigest.getInstance("SHA-1").digest(spki.getPublicKeyData().getBytes()));
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 is not available", ex);
        }
    }

    @Override
    public byte[] value() {
        return value.clone();
    }

    public String toHex() {
        return HexFormat.of().withUpperCase().formatHex(value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof KeyIdentifier that && Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return toHex();
    }

}
