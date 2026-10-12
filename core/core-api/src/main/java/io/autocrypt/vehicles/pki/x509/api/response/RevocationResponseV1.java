package io.autocrypt.vehicles.pki.x509.api.response;

/**
 * v1 폐기는 실패도 이 본문으로 알린다 (400 업무 오류, 500 시스템 오류).
 */
public record RevocationResponseV1(boolean revoked, String failedReason) {

    public static RevocationResponseV1 succeed() {
        return new RevocationResponseV1(true, null);
    }

    public static RevocationResponseV1 failed(String reason) {
        return new RevocationResponseV1(false, reason);
    }

}
