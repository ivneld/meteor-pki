package io.autocrypt.vehicles.pki.x509.domain;

/**
 * 목록 조회의 페이지 번호(0부터)와 크기. 값이 맞지 않으면 IllegalArgumentException(400)이다.
 */
public record PageQuery(int page, int size) {

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one");
        }
    }

}
