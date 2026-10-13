package com.meteor.pki.domain;

import java.util.List;
import java.util.Set;

import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;

public record CertProfileDraft(String name, CertType certType, ValidityDuration validityDuration,
        Integer pathLenConstraint, SubjectAltNameTemplate subjectAltNameTemplate, Set<KeyUsage> keyUsages,
        boolean keyUsageCritical, List<ProfileEku> extendedKeyUsages, CertificatePolicies certificatePolicies,
        List<String> ocspUris, boolean useDefaultOcspUri, String defaultOcspUri, boolean requireApproval,
        boolean useDefaultAiaCaIssuer, KeyAlgorithm keyAlgorithm, List<String> customExtensionOids) {

}
