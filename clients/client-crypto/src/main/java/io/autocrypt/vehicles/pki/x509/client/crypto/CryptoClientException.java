package io.autocrypt.vehicles.pki.x509.client.crypto;

/**
 * 키 생성이나 서명이 실패했다. 이 모듈은 core-api 의 에러 어휘를 모르므로 자체 예외로 알리고, 에러 코드로 바꾸는 일은 호출하는 usecase 가
 * 한다.
 */
public class CryptoClientException extends RuntimeException {

    public CryptoClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoClientException(String message) {
        super(message);
    }

}
