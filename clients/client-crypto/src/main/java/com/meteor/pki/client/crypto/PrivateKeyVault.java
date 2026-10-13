package com.meteor.pki.client.crypto;

import java.security.PrivateKey;

interface PrivateKeyVault {

    void store(String keyId, PrivateKey privateKey);

    PrivateKey load(String keyId);

}
