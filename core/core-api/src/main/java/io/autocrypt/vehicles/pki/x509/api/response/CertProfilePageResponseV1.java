package io.autocrypt.vehicles.pki.x509.api.response;

import java.util.List;

public record CertProfilePageResponseV1(List<CertProfileDetailResponseV1> certProfileDetailResponses, int page,
        int size, long totalElements, int totalPages, int blockStart, int blockEnd) {
}
