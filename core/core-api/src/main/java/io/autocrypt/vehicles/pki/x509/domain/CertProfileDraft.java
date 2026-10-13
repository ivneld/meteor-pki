package io.autocrypt.vehicles.pki.x509.domain;

import java.util.List;
import java.util.Set;

import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

/**
 * 프로파일을 만들 때 받는 값. 요청 그대로이며, 인증서 종류에 따라 무엇을 쓰고 버릴지는 {@link CertProfile#create} 가 정한다.
 *
 * @param pathLenConstraint SUB 에서만 쓴다. 없으면 0
 * @param certificatePolicies 없으면 null (확장을 넣지 않음)
 * @param ocspUris 기본 OCSP 주소를 뺀 요청 목록
 * @param defaultOcspUri useDefaultOcspUri 일 때 앞에 붙일 기본 OCSP 주소
 * @param customExtensionOids 요청한 사용자 정의 확장의 OID
 */
public record CertProfileDraft(String name, CertType certType, ValidityDuration validityDuration,
        Integer pathLenConstraint, SubjectAltNameTemplate subjectAltNameTemplate, Set<KeyUsage> keyUsages,
        boolean keyUsageCritical, List<ProfileEku> extendedKeyUsages, CertificatePolicies certificatePolicies,
        List<String> ocspUris, boolean useDefaultOcspUri, String defaultOcspUri, boolean requireApproval,
        boolean useDefaultAiaCaIssuer, KeyAlgorithm keyAlgorithm, List<String> customExtensionOids) {

}
