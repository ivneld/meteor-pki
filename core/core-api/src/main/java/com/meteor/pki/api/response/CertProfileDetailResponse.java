package com.meteor.pki.api.response;

import java.time.Instant;
import java.util.List;

import com.meteor.pki.domain.CertProfile;
import com.meteor.pki.domain.CertificatePolicies;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;

public record CertProfileDetailResponse(Long id, String name, CertType certType, String validityPeriod, boolean isCa,
        Integer pathLenConstraint, boolean keyUsageCritical, boolean requireApproval, List<KeyUsage> keyUsages,
        List<CertProfileEkuResponse> extendedKeyUsages, String certificateOidsCsv, boolean certificatePoliciesCritical,
        String sanCsv, String keyAlgorithm, boolean useDefaultAiaCaIssuer, List<String> ocspUris,
        List<CertProfileCustomExtensionResponse> profileCustomExtensions, Instant createdAt, Instant updatedAt) {

    public static CertProfileDetailResponse from(CertProfile profile) {
        CertificatePolicies policies = profile.getCertificatePolicies();
        return new CertProfileDetailResponse(profile.getId(), profile.getName(), profile.getCertType(),
                profile.getValidityDuration().toPeriodString(), profile.isCa(), profile.getPathLenConstraint(),
                profile.isKeyUsageCritical(), profile.isRequireApproval(),
                profile.getKeyUsages().stream().sorted().toList(),
                profile.getExtendedKeyUsages().stream().map(CertProfileEkuResponse::from).toList(),
                policies == null ? "" : String.join(",", policies.oids()), policies != null && policies.critical(),
                profile.getSubjectAltNameTemplate().csv(), profile.getKeyAlgorithm().value(),
                profile.isUseDefaultAiaCaIssuer(), profile.getOcspUris(), List.of(), profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

}
