package io.autocrypt.vehicles.pki.x509.api.response;

import io.autocrypt.vehicles.pki.x509.enums.CustomOidValueType;

public record CustomOidExtensionResponse(String oid, boolean critical, String oidValue, CustomOidValueType valueType) {
}
