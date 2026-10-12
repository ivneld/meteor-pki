package com.meteor.client.pg;

/**
 * PG 승인 결과. 외부 응답 DTO 를 그대로 노출하지 않고 호출 측이 필요한 값만 담는다.
 */
public record PgApproval(String transactionKey) {
}
