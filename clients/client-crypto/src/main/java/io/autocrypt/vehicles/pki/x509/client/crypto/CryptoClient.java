package io.autocrypt.vehicles.pki.x509.client.crypto;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

import io.autocrypt.vehicles.pki.x509.enums.KeyAlgorithmSpec;
import io.autocrypt.vehicles.pki.x509.enums.SignatureAlgorithm;
import org.bouncycastle.jcajce.spec.MLDSAParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 키 생성과 서명의 공개 진입점. 운영에서 HSM/KMS 가 맡을 자리라 외부 시스템으로 다룬다. 개인키는 이 모듈 밖으로 나가지 않고, 호출자는 키 id 와
 * 공개키(SubjectPublicKeyInfo DER)만 안다.
 *
 * <p>
 * 외부 I/O 이므로 트랜잭션 안에서 부르지 않는다. {@code client.crypto.key-dir} 가 있으면 그 디렉터리에 암호화된 파일로,
 * 없으면(local 프로필) 메모리에 둔다.
 */
@Component
public class CryptoClient {

    private final Provider provider = new BouncyCastleProvider();

    private final PrivateKeyVault vault;

    CryptoClient(@Value("${client.crypto.key-dir:}") String keyDir,
            @Value("${client.crypto.key-password:changeit}") String keyPassword) {
        this.vault = keyDir == null || keyDir.isBlank() ? new InMemoryPrivateKeyVault()
                : new FilePrivateKeyVault(Path.of(keyDir), keyPassword.toCharArray(), provider);
    }

    /**
     * 키 쌍을 만들어 개인키를 keyId 로 보관하고 공개키를 돌려준다.
     * @return 공개키의 SubjectPublicKeyInfo DER
     */
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

    /** keyId 의 개인키로 data 에 서명한다. */
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
