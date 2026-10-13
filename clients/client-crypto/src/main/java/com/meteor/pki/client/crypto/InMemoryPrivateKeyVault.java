package com.meteor.pki.client.crypto;

import java.security.PrivateKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
