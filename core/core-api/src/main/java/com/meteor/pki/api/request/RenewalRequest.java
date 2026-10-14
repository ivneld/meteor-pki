package com.meteor.pki.api.request;

import com.meteor.pki.domain.KeyIdentifier;
import com.meteor.pki.domain.SerialNumber;
import com.meteor.pki.domain.SubjectPublicKey;
import com.meteor.pki.domain.ValidityDuration;
import com.meteor.pki.usecase.LeafRenewCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RenewalRequest(@NotBlank String serialNumberHex, @Min(0) int activationStartDaysAgo,
        boolean revokeOriginalCertificate, byte[] subjectPublicKeyInfoDer, @Pattern(regexp = "^$|^P[1-9]\\d*[YMD]$",
                message = "validityPeriod only allows PnY, PnM, or PnD format.") String validityPeriod) {

    public LeafRenewCommand toCommand(String akiHex) {
        return new LeafRenewCommand(SerialNumber.of(serialNumberHex), KeyIdentifier.of(akiHex), activationStartDaysAgo,
                validityPeriod == null || validityPeriod.isEmpty() ? null : ValidityDuration.from(validityPeriod),
                revokeOriginalCertificate,
                subjectPublicKeyInfoDer == null ? null : new SubjectPublicKey(subjectPublicKeyInfoDer));
    }

}
