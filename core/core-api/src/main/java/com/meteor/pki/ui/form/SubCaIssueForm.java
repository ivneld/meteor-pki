package com.meteor.pki.ui.form;

import java.io.Serializable;

import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.usecase.SubCaIssueCommand;

public record SubCaIssueForm(Long certProfileId, String subjectDn, String akiHex, String crlDps,
        Boolean includeDefaultCrlDp, String aiaCaIssuers, String ocspUris, String validityPeriod,
        String notBefore) implements Serializable {

    public static SubCaIssueForm empty(String akiHex) {
        return new SubCaIssueForm(null, "", akiHex, "", true, "", "", "", "");
    }

    public SubCaIssueCommand toCommand() {
        return new SubCaIssueCommand(certProfileId, DistinguishedName.of(subjectDn), KeyIdentifier.of(akiHex),
                FormValues.lines(crlDps), FormValues.checked(includeDefaultCrlDp), FormValues.lines(aiaCaIssuers),
                FormValues.lines(ocspUris), FormValues.duration(validityPeriod), FormValues.instant(notBefore));
    }

}
