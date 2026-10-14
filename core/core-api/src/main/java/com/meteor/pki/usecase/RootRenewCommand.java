package com.meteor.pki.usecase;

import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.RenewalOptions;
import com.meteor.pki.domain.SerialNumber;
import com.meteor.pki.domain.ValidityDuration;

public record RootRenewCommand(SerialNumber serialNumber, KeyIdentifier authorityKeyId, int activationStartDaysAgo,
        ValidityDuration validityDuration) {

    RenewalOptions options() {
        return new RenewalOptions(activationStartDaysAgo, validityDuration);
    }

}
