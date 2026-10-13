package io.autocrypt.vehicles.pki.x509.domain;

/**
 * 프로파일에 들어간 확장 키 용도(EKU). 프로파일을 만들 때 등록된 {@link Eku} 를 복사해 두므로 나중에 EKU 의 설명이 바뀌어도 프로파일은
 * 바뀌지 않는다.
 */
public record ProfileEku(String oid, String purpose) {

}
