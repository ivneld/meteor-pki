package com.meteor.pki.ui.form;

import java.io.Serializable;

import com.meteor.pki.domain.CertificationRequest;
import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SubjectPublicKey;
import com.meteor.pki.usecase.LeafIssueCommand;
import com.meteor.pki.usecase.LeafP10crIssueCommand;

public record LeafIssueForm(String mode, Long certProfileId, String akiHex, String subjectDn, String publicKeyPem,
        String csrPem, String crlDps, Boolean includeDefaultCrlDp, String aiaCaIssuers, String ocspUris,
        String validityPeriod, String notBefore) implements Serializable {

    public static LeafIssueForm empty(String akiHex) {
        return new LeafIssueForm("csr", null, akiHex, "", "", "", "", true, "", "", "", "");
    }

    public boolean usesCsr() {
        return !"key".equals(mode);
    }

    public LeafIssueCommand toKeyCommand() {
        SubjectPublicKey publicKey = FormValues.publicKey(publicKeyPem);
        if (publicKey == null) {
            throw new IllegalArgumentException("공개키를 입력하세요.");
        }
        return new LeafIssueCommand(certProfileId, DistinguishedName.of(subjectDn), KeyIdentifier.of(akiHex), publicKey,
                FormValues.lines(crlDps), FormValues.checked(includeDefaultCrlDp), FormValues.lines(aiaCaIssuers),
                FormValues.lines(ocspUris), FormValues.duration(validityPeriod), FormValues.instant(notBefore));
    }

    public LeafP10crIssueCommand toCsrCommand() {
        if (FormValues.blank(csrPem)) {
            throw new IllegalArgumentException("CSR(PEM)을 입력하세요.");
        }
        return new LeafP10crIssueCommand(certProfileId, CertificationRequest.fromPem(csrPem.trim()),
                KeyIdentifier.of(akiHex), FormValues.lines(crlDps), FormValues.checked(includeDefaultCrlDp),
                FormValues.lines(aiaCaIssuers), FormValues.lines(ocspUris), FormValues.duration(validityPeriod),
                FormValues.instant(notBefore));
    }

}
