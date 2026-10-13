package io.autocrypt.vehicles.pki.x509.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

/**
 * 키 용도 enum 과 keyUsage 확장 비트(RFC 5280 4.2.1.3) 사이의 변환.
 */
final class KeyUsageBits {

    private static final Map<KeyUsage, Integer> BITS = Map.of(KeyUsage.DIGITAL_SIGNATURE,
            org.bouncycastle.asn1.x509.KeyUsage.digitalSignature, KeyUsage.NON_REPUDIATION,
            org.bouncycastle.asn1.x509.KeyUsage.nonRepudiation, KeyUsage.KEY_ENCIPHERMENT,
            org.bouncycastle.asn1.x509.KeyUsage.keyEncipherment, KeyUsage.DATA_ENCIPHERMENT,
            org.bouncycastle.asn1.x509.KeyUsage.dataEncipherment, KeyUsage.KEY_AGREEMENT,
            org.bouncycastle.asn1.x509.KeyUsage.keyAgreement, KeyUsage.KEY_CERT_SIGN,
            org.bouncycastle.asn1.x509.KeyUsage.keyCertSign, KeyUsage.CRL_SIGN,
            org.bouncycastle.asn1.x509.KeyUsage.cRLSign, KeyUsage.ENCIPHER_ONLY,
            org.bouncycastle.asn1.x509.KeyUsage.encipherOnly, KeyUsage.DECIPHER_ONLY,
            org.bouncycastle.asn1.x509.KeyUsage.decipherOnly);

    private KeyUsageBits() {
    }

    static org.bouncycastle.asn1.x509.KeyUsage toExtension(Set<KeyUsage> usages) {
        int bits = 0;
        for (KeyUsage usage : usages) {
            bits |= BITS.get(usage);
        }
        return new org.bouncycastle.asn1.x509.KeyUsage(bits);
    }

    /** enum 선언 순서. */
    static List<KeyUsage> fromExtension(org.bouncycastle.asn1.x509.KeyUsage extension) {
        Set<KeyUsage> usages = EnumSet.noneOf(KeyUsage.class);
        BITS.forEach((usage, bit) -> {
            if (extension.hasUsages(bit)) {
                usages.add(usage);
            }
        });
        return List.copyOf(usages);
    }

}
