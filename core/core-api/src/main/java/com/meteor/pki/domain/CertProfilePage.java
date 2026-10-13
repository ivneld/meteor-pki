package com.meteor.pki.domain;

import java.util.List;

public record CertProfilePage(List<CertProfile> profiles, int page, int size, long totalElements, int totalPages) {

    private static final int BLOCK_SIZE = 5;

    public CertProfilePage {
        profiles = List.copyOf(profiles);
    }

    public int blockStart() {
        return (page / BLOCK_SIZE) * BLOCK_SIZE;
    }

    public int blockEnd() {
        return Math.min(blockStart() + BLOCK_SIZE - 1, totalPages - 1);
    }

}
