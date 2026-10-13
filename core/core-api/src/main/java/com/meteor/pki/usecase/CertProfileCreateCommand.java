package com.meteor.pki.usecase;

import java.util.List;
import java.util.Set;

import com.meteor.pki.domain.CertProfileDraft;
import com.meteor.pki.domain.CertificatePolicies;
import com.meteor.pki.domain.KeyAlgorithm;
import com.meteor.pki.domain.ProfileEku;
import com.meteor.pki.domain.SubjectAltNameTemplate;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;

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
