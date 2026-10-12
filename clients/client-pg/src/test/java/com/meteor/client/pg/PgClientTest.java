package com.meteor.client.pg;

import com.meteor.ClientPgContextTest;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = "client.pg.fake=false")
class PgClientTest extends ClientPgContextTest {

    private final PgClient pgClient;

    PgClientTest(PgClient pgClient) {
        this.pgClient = pgClient;
    }

    @Test
    void wrapsFeignFailureAgainstUnreachableHost() {
        // 존재하지 않는 호스트(.invalid)를 바라보므로 Feign 예외가 이 모듈의 예외로 바뀌어야 한다.
        assertThatThrownBy(() -> pgClient.approve("1", 10_000)).isInstanceOf(PgApprovalException.class);
    }

}
