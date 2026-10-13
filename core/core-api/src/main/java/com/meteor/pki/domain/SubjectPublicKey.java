package com.meteor.pki.domain;

import java.io.IOException;
import java.util.Arrays;

import com.meteor.pki.enums.KeyAlgorithmSpec;
import com.meteor.pki.support.error.CoreException;
import com.meteor.pki.support.error.ErrorCode;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;

public record SubjectPublicKey(byte[] der) {

    public SubjectPublicKey {
        if (der == null || der.length == 0) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "SubjectPublicKeyInfo must not be empty.");
        }
        der = der.clone();
        parse(der);
    }

    public static SubjectPublicKey from(SubjectPublicKeyInfo spki) {
        try {
            return new SubjectPublicKey(spki.getEncoded());
        }
        catch (IOException ex) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid SubjectPublicKeyInfo.");
        }
    }

    @Override
    public byte[] der() {
        return der.clone();
    }

    public SubjectPublicKeyInfo toSpki() {
        return parse(der);
    }

    public KeyIdentifier keyIdentifier() {
        return KeyIdentifier.fromSpki(toSpki());
    }

    public KeyAlgorithm keyAlgorithm() {
        SubjectPublicKeyInfo spki = toSpki();
        ASN1ObjectIdentifier algorithm = spki.getAlgorithm().getAlgorithm();
        if (algorithm.equals(PKCSObjectIdentifiers.rsaEncryption)
                || algorithm.equals(PKCSObjectIdentifiers.id_RSASSA_PSS)) {
            return new KeyAlgorithm(rsaSpec(spki));
        }
        if (algorithm.equals(X9ObjectIdentifiers.id_ecPublicKey)) {
            String curve = String.valueOf(spki.getAlgorithm().getParameters());
            return new KeyAlgorithm(Arrays.stream(KeyAlgorithmSpec.values())
                .filter(spec -> curve.equals(spec.getCurveOid()))
                .findFirst()
                .orElse(KeyAlgorithmSpec.EC_P256));
        }
        return Arrays.stream(KeyAlgorithmSpec.values())
            .filter(spec -> spec.getCurveOid() == null && spec.getAlgorithmOid().equals(algorithm.getId())
                    && !spec.getAlgorithmOid().equals(PKCSObjectIdentifiers.rsaEncryption.getId()))
            .findFirst()
            .map(KeyAlgorithm::new)
            .orElseThrow(() -> new CoreException(ErrorCode.CERT_INVALID_INPUT, "Unsupported SPKI algorithm OID."));
    }

    private static KeyAlgorithmSpec rsaSpec(SubjectPublicKeyInfo spki) {
        int bits;
        try {
            bits = RSAPublicKey.getInstance(spki.parsePublicKey()).getModulus().bitLength();
        }
        catch (IOException | IllegalArgumentException ex) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid RSA SubjectPublicKeyInfo");
        }
        if (bits >= 4064) {
            return KeyAlgorithmSpec.RSA_4096;
        }
        if (bits >= 3040) {
            return KeyAlgorithmSpec.RSA_3072;
        }
        if (bits >= 2016) {
            return KeyAlgorithmSpec.RSA_2048;
        }
        throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Unsupported RSA key size.");
    }

    private static SubjectPublicKeyInfo parse(byte[] der) {
        try {
            return SubjectPublicKeyInfo.getInstance(der);
        }
        catch (IllegalArgumentException | ClassCastException ex) {
            throw new CoreException(ErrorCode.CERT_INVALID_INPUT, "Invalid SubjectPublicKeyInfo.");
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SubjectPublicKey that && Arrays.equals(der, that.der);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(der);
    }

    @Override
    public String toString() {
        return "SubjectPublicKey[" + keyIdentifier() + "]";
    }

}
