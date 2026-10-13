package com.meteor.pki.client.crypto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import com.meteor.pki.enums.KeyAlgorithmSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoClientTest {

    private static final byte[] DATA = "tbs".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest
    @EnumSource(value = KeyAlgorithmSpec.class,
            names = { "RSA_2048", "EC_P256", "EC_P384", "EC_P521", "ML_DSA_44", "ED448" })
    void signatureVerifiesWithTheReturnedPublicKey(KeyAlgorithmSpec spec) throws Exception {
        CryptoClient client = new CryptoClient("", "changeit");

        byte[] spki = client.generateKeyPair("k-" + spec, spec);
        byte[] signature = client.sign("k-" + spec, spec.getSignatureAlgorithm(), DATA);

        assertThat(verify(spki, spec, signature)).isTrue();
    }

    @Test
    void fileVaultKeepsEncryptedKeysAcrossInstances(@TempDir Path dir) throws Exception {
        byte[] spki = new CryptoClient(dir.toString(), "secret").generateKeyPair("k-file", KeyAlgorithmSpec.EC_P256);

        byte[] signature = new CryptoClient(dir.toString(), "secret").sign("k-file",
                KeyAlgorithmSpec.EC_P256.getSignatureAlgorithm(), DATA);

        assertThat(verify(spki, KeyAlgorithmSpec.EC_P256, signature)).isTrue();
        assertThat(Files.readString(dir.resolve("k-file.p8"))).contains("ENCRYPTED PRIVATE KEY");
        assertThatThrownBy(() -> new CryptoClient(dir.toString(), "wrong").sign("k-file",
                KeyAlgorithmSpec.EC_P256.getSignatureAlgorithm(), DATA))
            .isInstanceOf(CryptoClientException.class);
    }

    @Test
    void keyIdsAreUniqueAndMustExist() {
        CryptoClient client = new CryptoClient("", "changeit");
        client.generateKeyPair("k-dup", KeyAlgorithmSpec.EC_P256);

        assertThatThrownBy(() -> client.generateKeyPair("k-dup", KeyAlgorithmSpec.EC_P256))
            .isInstanceOf(CryptoClientException.class);
        assertThatThrownBy(() -> client.sign("k-missing", KeyAlgorithmSpec.EC_P256.getSignatureAlgorithm(), DATA))
            .isInstanceOf(CryptoClientException.class);
    }

    private static boolean verify(byte[] spki, KeyAlgorithmSpec spec, byte[] signature) throws Exception {
        BouncyCastleProvider provider = new BouncyCastleProvider();
        String algorithm = switch (spec) {
            case RSA_2048, RSA_3072, RSA_4096 -> "RSA";
            case ML_DSA_44 -> "ML-DSA";
            case ED448 -> "Ed448";
            default -> "EC";
        };
        PublicKey key = KeyFactory.getInstance(algorithm, provider).generatePublic(new X509EncodedKeySpec(spki));
        Signature verifier = Signature.getInstance(spec.getSignatureAlgorithm().getJcaName(), provider);
        verifier.initVerify(key);
        verifier.update(DATA);
        return verifier.verify(signature);
    }

}
