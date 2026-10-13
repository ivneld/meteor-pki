package com.meteor.pki.support.storage;

import java.sql.Connection;

import javax.sql.DataSource;

import com.meteor.pki.CoreDbContextTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
