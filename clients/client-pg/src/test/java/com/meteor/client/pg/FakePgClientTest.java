package com.meteor.client.pg;

import com.meteor.ClientPgContextTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FakePgClientTest extends ClientPgContextTest {

    private final PgClient pgClient;

    FakePgClientTest(PgClient pgClient) {
        this.pgClient = pgClient;
    }

    @Test
    void localProfileApprovesWithoutPg() {
        assertThat(pgClient.approve("1", 10_000).transactionKey()).isEqualTo("fake-1");
    }

}
