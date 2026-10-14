package com.meteor.pki.ui.form;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.meteor.pki.domain.CertificatePolicies;
import com.meteor.pki.domain.KeyAlgorithm;
import com.meteor.pki.domain.SubjectAltNameTemplate;
import com.meteor.pki.enums.CertType;
import com.meteor.pki.enums.KeyUsage;
import com.meteor.pki.usecase.CertProfileCreateCommand;

public record ProfileForm(String name, CertType certType, String validityPeriod, Integer pathLenConstraint,
        String subjectAltName, List<KeyUsage> keyUsages, Boolean keyUsageCritical, List<String> ekuOids,
        String policyOids, String ocspUris, Boolean useDefaultOcspUri, Boolean requireApproval,
        Boolean useDefaultAiaCaIssuer, String keyAlgorithmSpec) implements Serializable {

    public static ProfileForm empty(CertType certType) {
        return new ProfileForm("", certType, "P1Y", null, "", List.of(), false, List.of(), "", "", true, false, true,
                "EC-P-256");
    }

    public boolean hasKeyUsage(KeyUsage keyUsage) {
        return keyUsages != null && keyUsages.contains(keyUsage);
    }

    public boolean hasEku(String oid) {
        return ekuOids != null && ekuOids.contains(oid);
    }

    public CertProfileCreateCommand toCommand() {
        if (FormValues.blank(name)) {
            throw new IllegalArgumentException("프로파일 이름을 입력하세요.");
        }
        if (certType == null) {
            throw new IllegalArgumentException("인증서 종류를 선택하세요.");
        }
        if (FormValues.blank(validityPeriod)) {
            throw new IllegalArgumentException("유효기간을 입력하세요.");
        }
        Set<KeyUsage> usages = keyUsages == null || keyUsages.isEmpty() ? EnumSet.noneOf(KeyUsage.class)
                : EnumSet.copyOf(keyUsages);
        List<String> policies = FormValues.lines(policyOids);
        return new CertProfileCreateCommand(name.trim(), certType, FormValues.duration(validityPeriod),
                certType == CertType.LEAF ? null : pathLenConstraint, SubjectAltNameTemplate.of(subjectAltName), usages,
                FormValues.checked(keyUsageCritical), ekuOids == null ? List.of() : ekuOids,
                CertificatePolicies.ofNullable(policies.isEmpty() ? null : policies), FormValues.lines(ocspUris),
                FormValues.checked(useDefaultOcspUri), FormValues.checked(requireApproval),
                FormValues.checked(useDefaultAiaCaIssuer), KeyAlgorithm.of(keyAlgorithmSpec), List.of());
    }

}
