package com.meteor.pki.ui.form;

import java.io.Serializable;

import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SerialNumber;
import com.meteor.pki.usecase.LeafRenewCommand;
import com.meteor.pki.usecase.RootRenewCommand;
import com.meteor.pki.usecase.SubCaRenewCommand;

public record RenewForm(Integer activationStartDaysAgo, String validityPeriod, Boolean revokeOriginalCertificate,
        Boolean useLatestIssuer, String publicKeyPem) implements Serializable {

    public RootRenewCommand toRootCommand(String akiHex, String serialHex) {
        return new RootRenewCommand(SerialNumber.of(serialHex), KeyIdentifier.of(akiHex), daysAgo(),
                FormValues.duration(validityPeriod));
    }

    public SubCaRenewCommand toSubCaCommand(String akiHex, String serialHex) {
        return new SubCaRenewCommand(SerialNumber.of(serialHex), KeyIdentifier.of(akiHex), daysAgo(),
                FormValues.duration(validityPeriod), FormValues.checked(revokeOriginalCertificate),
                FormValues.checked(useLatestIssuer));
    }

    public LeafRenewCommand toLeafCommand(String akiHex, String serialHex) {
        return new LeafRenewCommand(SerialNumber.of(serialHex), KeyIdentifier.of(akiHex), daysAgo(),
                FormValues.duration(validityPeriod), FormValues.checked(revokeOriginalCertificate),
                FormValues.publicKey(publicKeyPem));
    }

    private int daysAgo() {
        return activationStartDaysAgo == null ? 0 : activationStartDaysAgo;
    }

}
