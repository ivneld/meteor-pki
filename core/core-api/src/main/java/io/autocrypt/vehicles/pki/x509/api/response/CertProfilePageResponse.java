package io.autocrypt.vehicles.pki.x509.api.response;

import java.util.List;

public record CertProfilePageResponse(List<CertProfileDetailResponse> certProfileDetailResponses, int page, int size,
        long totalElements, int totalPages, int blockStart, int blockEnd) {
}
