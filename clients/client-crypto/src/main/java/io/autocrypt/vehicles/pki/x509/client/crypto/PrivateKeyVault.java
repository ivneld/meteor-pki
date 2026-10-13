package io.autocrypt.vehicles.pki.x509.client.crypto;

import java.security.PrivateKey;

/**
 * 개인키 보관소. 키 id 로 넣고 꺼낸다. 같은 id 를 두 번 넣을 수 없다.
 */
interface PrivateKeyVault {

    void store(String keyId, PrivateKey privateKey);

    PrivateKey load(String keyId);

}
