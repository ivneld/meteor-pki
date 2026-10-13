package com.meteor.pki.domain;

import java.time.Instant;
import java.util.UUID;

public record PrivateKeyId(String value) {

    public PrivateKeyId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Private key id must not be blank.");
        }
    }

    public static PrivateKeyId generate(Instant now) {
        return new PrivateKeyId("k-" + Long.toString(now.getEpochSecond(), 36) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    }

}
