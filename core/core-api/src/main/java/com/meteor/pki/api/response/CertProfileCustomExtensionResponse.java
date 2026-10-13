package com.meteor.pki.api.response;

import com.meteor.pki.enums.CustomOidValueType;

public record CertProfileCustomExtensionResponse(String oid, boolean critical, String oidValue,
        CustomOidValueType valueType) {
}
