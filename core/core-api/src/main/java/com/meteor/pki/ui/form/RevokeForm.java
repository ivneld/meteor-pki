package com.meteor.pki.ui.form;

import java.io.Serializable;

import com.meteor.pki.domain.Revocation;
import com.meteor.pki.enums.CrlReason;

public record RevokeForm(Integer reason) implements Serializable {

    public CrlReason crlReason() {
        if (reason == null) {
            throw new IllegalArgumentException("폐기 사유를 선택하세요.");
        }
        return Revocation.reasonOf(reason);
    }

}
