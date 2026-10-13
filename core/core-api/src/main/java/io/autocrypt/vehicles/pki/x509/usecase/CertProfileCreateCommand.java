package io.autocrypt.vehicles.pki.x509.usecase;

import java.util.List;
import java.util.Set;

import io.autocrypt.vehicles.pki.x509.domain.CertProfileDraft;
import io.autocrypt.vehicles.pki.x509.domain.CertificatePolicies;
import io.autocrypt.vehicles.pki.x509.domain.KeyAlgorithm;
import io.autocrypt.vehicles.pki.x509.domain.ProfileEku;
import io.autocrypt.vehicles.pki.x509.domain.SubjectAltNameTemplate;
import io.autocrypt.vehicles.pki.x509.domain.ValidityDuration;
import io.autocrypt.vehicles.pki.x509.enums.CertType;
import io.autocrypt.vehicles.pki.x509.enums.KeyUsage;

/**
 * @param ekuOids 프로파일에 넣을 EKU 의 OID. 모두 등록된 것이어야 한다
 * @param certificatePolicies 없으면 null
 */
public record CertProfileCreateCommand(String name, CertType certType, ValidityDuration validityDuration,
        Integer pathLenConstraint, SubjectAltNameTemplate subjectAltNameTemplate, Set<KeyUsage> keyUsages,
        boolean keyUsageCritical, List<String> ekuOids, CertificatePolicies certificatePolicies, List<String> ocspUris,
        boolean useDefaultOcspUri, boolean requireApproval, boolean useDefaultAiaCaIssuer, KeyAlgorithm keyAlgorithm,
        List<String> customExtensionOids) {

    CertProfileDraft toDraft(List<ProfileEku> extendedKeyUsages, String defaultOcspUri) {
        return new CertProfileDraft(name, certType, validityDuration, pathLenConstraint, subjectAltNameTemplate,
                keyUsages, keyUsageCritical, extendedKeyUsages, certificatePolicies, ocspUris, useDefaultOcspUri,
                defaultOcspUri, requireApproval, useDefaultAiaCaIssuer, keyAlgorithm, customExtensionOids);
    }

}
