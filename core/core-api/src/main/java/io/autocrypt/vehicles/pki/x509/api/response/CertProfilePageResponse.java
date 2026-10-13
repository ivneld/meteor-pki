package io.autocrypt.vehicles.pki.x509.api.response;

import java.util.List;

import io.autocrypt.vehicles.pki.x509.domain.CertProfilePage;

public record CertProfilePageResponse(List<CertProfileDetailResponse> certProfileDetailResponses, int page, int size,
        long totalElements, int totalPages, int blockStart, int blockEnd) {

    public static CertProfilePageResponse from(CertProfilePage page) {
        return new CertProfilePageResponse(page.profiles().stream().map(CertProfileDetailResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages(), page.blockStart(), page.blockEnd());
    }

}
