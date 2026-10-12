package com.meteor.support.storage;

import java.sql.Connection;

import javax.sql.DataSource;

import com.meteor.CoreDbContextTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이 모듈의 책임은 저장소 인프라뿐이므로, db-core.yml 설정으로 DataSource 가 뜨고 연결되는지만 확인한다. 엔티티와 *Repository 의
 * 왕복 테스트는 core-api 의 domain.*RepositoryIT 에 있다.
 */
class CoreDataSourceIT extends CoreDbContextTest {

    private final DataSource dataSource;

    CoreDataSourceIT(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Test
    void connects() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
        }
    }

}
