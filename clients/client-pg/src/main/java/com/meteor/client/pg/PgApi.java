package com.meteor.client.pg;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * PG 의 HTTP 계약. 모듈 밖으로는 노출하지 않고 {@link PgClient} 를 통해서만 사용한다.
 */
@FeignClient(value = "pg-api", url = "${client.pg.url}")
interface PgApi {

    @PostMapping(value = "/v1/approvals", consumes = MediaType.APPLICATION_JSON_VALUE)
    PgApproveResponse approve(@RequestBody PgApproveRequest request);

}
