package com.meteor.pki.api.response;

public record RenewalResponse(String serialNumberHex, String subjectDn, String certificateDerBase64) {
}
