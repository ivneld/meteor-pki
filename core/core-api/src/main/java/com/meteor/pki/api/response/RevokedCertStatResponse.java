package com.meteor.pki.api.response;

public record RevokedCertStatResponse(String groupBy, Long revokedCertCount) {
}
