package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;

/**
 * 프로파일 목록의 한 페이지. 화면 페이지 버튼을 5개씩 묶는 블록 범위도 계산한다(x509-ra 와 같다).
 */
public record CertProfilePage(List<CertProfile> profiles, int page, int size, long totalElements, int totalPages) {

    private static final int BLOCK_SIZE = 5;

    public CertProfilePage {
        profiles = List.copyOf(profiles);
    }

    public int blockStart() {
        return (page / BLOCK_SIZE) * BLOCK_SIZE;
    }

    /** 블록의 마지막 페이지. 페이지가 하나도 없으면 -1. */
    public int blockEnd() {
        return Math.min(blockStart() + BLOCK_SIZE - 1, totalPages - 1);
    }

}
