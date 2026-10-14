package com.meteor.pki.ui.form;

import java.io.Serializable;

import com.meteor.pki.domain.DistinguishedName;
import com.meteor.pki.usecase.RootIssueCommand;

public record RootIssueForm(Long certProfileId, String subjectDn, String validityPeriod, String crlDps, String ocspUris,
        String notBefore) implements Serializable {

    public static RootIssueForm empty() {
        return new RootIssueForm(null, "", "", "", "", "");
    }

    public RootIssueCommand toCommand() {
        return new RootIssueCommand(certProfileId, DistinguishedName.of(subjectDn), FormValues.duration(validityPeriod),
                FormValues.lines(crlDps), FormValues.lines(ocspUris), FormValues.instant(notBefore));
    }

}
