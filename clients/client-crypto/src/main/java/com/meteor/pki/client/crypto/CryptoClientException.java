package com.meteor.pki.client.crypto;

public class CryptoClientException extends RuntimeException {

    public CryptoClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoClientException(String message) {
        super(message);
    }

}
