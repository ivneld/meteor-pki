package com.meteor.pki.ui.form;

import java.io.Serializable;

public record EkuForm(String oid, String purpose) implements Serializable {

    public static EkuForm empty() {
        return new EkuForm("", "");
    }

    public String trimmedOid() {
        if (FormValues.blank(oid) || FormValues.blank(purpose)) {
            throw new IllegalArgumentException("OID 와 용도를 모두 입력하세요.");
        }
        return oid.trim();
    }

    public String trimmedPurpose() {
        return purpose.trim();
    }

}
