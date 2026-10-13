package com.meteor.pki.client.crypto;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

import com.meteor.pki.enums.KeyAlgorithmSpec;
import com.meteor.pki.enums.SignatureAlgorithm;
import org.bouncycastle.jcajce.spec.MLDSAParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CryptoClient {

    private final Provider provider = new BouncyCastleProvider();

    private final PrivateKeyVault vault;

    CryptoClient(@Value("${client.crypto.key-dir:}") String keyDir,
            @Value("${client.crypto.key-password:changeit}") String keyPassword) {
        this.vault = keyDir == null || keyDir.isBlank() ? new InMemoryPrivateKeyVault()
                : new FilePrivateKeyVault(Path.of(keyDir), keyPassword.toCharArray(), provider);
    }

    public byte[] generateKeyPair(String keyId, KeyAlgorithmSpec spec) {
        try {
            KeyPair keyPair = generator(spec).generateKeyPair();
            vault.store(keyId, keyPair.getPrivate());
            return keyPair.getPublic().getEncoded();
        }
        catch (GeneralSecurityException ex) {
            throw new CryptoClientException("Key generation failed: " + spec, ex);
        }
    }

    public byte[] sign(String keyId, SignatureAlgorithm algorithm, byte[] data) {
        try {
            Signature signature = Signature.getInstance(algorithm.getJcaName(), provider);
            signature.initSign(vault.load(keyId));
            signature.update(data);
            return signature.sign();
        }
        catch (GeneralSecurityException ex) {
            throw new CryptoClientException("Signing failed: keyId=" + keyId + ", algorithm=" + algorithm, ex);
        }
    }

    private KeyPairGenerator generator(KeyAlgorithmSpec spec) throws GeneralSecurityException {
        return switch (spec) {
            case RSA_2048 -> rsa(2048);
            case RSA_3072 -> rsa(3072);
            case RSA_4096 -> rsa(4096);
            case EC_P256 -> ec("secp256r1");
            case EC_P384 -> ec("secp384r1");
            case EC_P521 -> ec("secp521r1");
            case ML_DSA_44 -> {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("ML-DSA", provider);
                generator.initialize(MLDSAParameterSpec.ml_dsa_44);
                yield generator;
            }
            case ED448 -> KeyPairGenerator.getInstance("Ed448", provider);
        };
    }

    private KeyPairGenerator rsa(int bits) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
        generator.initialize(bits);
        return generator;
    }

    private KeyPairGenerator ec(String curve) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", provider);
        generator.initialize(new ECGenParameterSpec(curve));
        return generator;
    }

}
