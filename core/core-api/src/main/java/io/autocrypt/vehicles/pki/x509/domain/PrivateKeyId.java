package io.autocrypt.vehicles.pki.x509.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 키 보관소(HSM/KMS 자리)에 둔 개인키의 이름. 개인키 자체는 이 서버의 DB 에 두지 않는다.
 */
public record PrivateKeyId(String value) {

    public PrivateKeyId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Private key id must not be blank.");
        }
    }

    /** {@code k-<생성 시각(초, 36진수)>-<난수 16자>}. x509-ra 와 같은 모양이다. */
    public static PrivateKeyId generate(Instant now) {
        return new PrivateKeyId("k-" + Long.toString(now.getEpochSecond(), 36) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    }

}
