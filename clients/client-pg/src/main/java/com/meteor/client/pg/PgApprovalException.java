package com.meteor.client.pg;

/**
 * PG 승인이 거절되거나 호출이 실패했다. 이 모듈은 core-api 의 에러 어휘를 모르므로 자체 예외로 알리고, 에러 코드로 바꾸는 일은 호출하는
 * usecase 가 한다.
 */
public class PgApprovalException extends RuntimeException {

    public PgApprovalException(String message, Throwable cause) {
        super(message, cause);
    }

}
