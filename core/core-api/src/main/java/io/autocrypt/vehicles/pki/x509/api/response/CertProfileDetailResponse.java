package io.autocrypt.vehicles.pki.x509.api.response;

import java.time.Instant;
import java.util.List;

import io.autocrypt.vehicles.pki.x509.domain.CertProfile;
import io.autocrypt.vehicles.pki.x509.domain.CertificatePolicies;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

/**
 * keyUsages 는 enum 선언 순서, certificateOidsCsv 는 정책 OID 를 쉼표로 이은 것(없으면 빈 문자열), keyAlgorithm
 * 은 표시값(EC-P-256)이다. 사용자 정의 확장은 아직 등록할 수 없어 항상 빈 목록이다.
 */
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
