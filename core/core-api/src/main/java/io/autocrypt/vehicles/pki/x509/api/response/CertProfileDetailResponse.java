package io.autocrypt.vehicles.pki.x509.api.response;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

public record CertProfileDetailResponse(Long id, String name, CertType certType, String validityPeriod, boolean isCa,
        Integer pathLenConstraint, boolean keyUsageCritical, boolean requireApproval, List<KeyUsage> keyUsages,
        List<CertProfileEkuResponse> extendedKeyUsages, String certificateOidsCsv, boolean certificatePoliciesCritical,
        String sanCsv, String keyAlgorithm, boolean useDefaultAiaCaIssuer, List<String> ocspUris,
        List<CertProfileCustomExtensionResponse> profileCustomExtensions, Instant createdAt, Instant updatedAt) {
}
