package io.autocrypt.vehicles.pki.x509.client.crypto;

import java.security.PrivateKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메모리에만 두는 보관소. 로컬 실행과 테스트용이며 재기동하면 키가 사라진다.
 */
class InMemoryPrivateKeyVault implements PrivateKeyVault {

    private final Map<String, PrivateKey> keys = new ConcurrentHashMap<>();

    @Override
    public void store(String keyId, PrivateKey privateKey) {
        if (keys.putIfAbsent(keyId, privateKey) != null) {
            throw new CryptoClientException("Key id already exists: " + keyId);
        }
    }

    @Override
    public PrivateKey load(String keyId) {
        PrivateKey key = keys.get(keyId);
        if (key == null) {
            throw new CryptoClientException("Key not found: " + keyId);
        }
        return key;
    }

}
