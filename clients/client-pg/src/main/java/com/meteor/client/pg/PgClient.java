package com.meteor.client.pg;

import feign.FeignException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PG 연동의 공개 진입점. 재시도·타임아웃·에러 변환 같은 정책은 이 클래스에 모은다.
 *
 * <p>
 * 외부 I/O 이므로 트랜잭션 안에서 부르지 않는다. 호출은 usecase 가 트랜잭션 경계 사이에서 한다(ARCHITECTURE.md 트랜잭션 나누기).
 * {@code client.pg.fake=true}(local 프로필)이면 PG 없이 승인 키를 만들어 돌려준다.
 */
@Component
public class PgClient {

    private final PgApi pgApi;

    private final boolean fake;

    PgClient(PgApi pgApi, @Value("${client.pg.fake:false}") boolean fake) {
        this.pgApi = pgApi;
        this.fake = fake;
    }

    public PgApproval approve(String paymentKey, long amount) {
        if (fake) {
            return new PgApproval("fake-" + paymentKey);
        }
        try {
            return pgApi.approve(new PgApproveRequest(paymentKey, amount)).toApproval();
        }
        catch (FeignException ex) {
            throw new PgApprovalException("pg approval failed: paymentKey=" + paymentKey, ex);
        }
    }

}
