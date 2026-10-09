// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.dao.sql.citus.CitusConnectionInitSqlPostProcessor.CITUS_CONNECTION_INIT_SQL;

class CitusConnectionInitSqlPostProcessorTest {

    private static final String DATA_SOURCE_BEAN_NAME = "dataSource";
    private static final String OPERATOR_INIT_SQL = "SET application_name TO 'tb'";

    private static CitusConnectionInitSqlPostProcessor postProcessor(boolean citusEnabled) {
        MockEnvironment environment = new MockEnvironment();
        if (citusEnabled) {
            environment.withProperty("database.citus.enabled", "true");
        }
        return new CitusConnectionInitSqlPostProcessor(environment, DATA_SOURCE_BEAN_NAME);
    }

    @Test
    void blankInitSqlIsReplacedWithCitusGucs() {
        CitusConnectionInitSqlPostProcessor processor = postProcessor(true);
        HikariDataSource dataSource = new HikariDataSource();

        processor.postProcessAfterInitialization(dataSource, DATA_SOURCE_BEAN_NAME);

        assertThat(dataSource.getConnectionInitSql()).isEqualTo(CITUS_CONNECTION_INIT_SQL);
    }

    @Test
    void operatorInitSqlIsPreservedAndCitusGucsAppendedLast() {
        CitusConnectionInitSqlPostProcessor processor = postProcessor(true);
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setConnectionInitSql(OPERATOR_INIT_SQL);

        processor.postProcessAfterInitialization(dataSource, DATA_SOURCE_BEAN_NAME);

        String result = dataSource.getConnectionInitSql();
        assertThat(result).startsWith(OPERATOR_INIT_SQL);
        assertThat(result).endsWith(CITUS_CONNECTION_INIT_SQL);
        assertThat(result).isEqualTo(OPERATOR_INIT_SQL + "; " + CITUS_CONNECTION_INIT_SQL);
    }

    @Test
    void secondApplicationDoesNotDuplicateGucs() {
        CitusConnectionInitSqlPostProcessor processor = postProcessor(true);
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setConnectionInitSql(OPERATOR_INIT_SQL);

        processor.postProcessAfterInitialization(dataSource, DATA_SOURCE_BEAN_NAME);
        String afterFirst = dataSource.getConnectionInitSql();
        processor.postProcessAfterInitialization(dataSource, DATA_SOURCE_BEAN_NAME);

        assertThat(dataSource.getConnectionInitSql()).isEqualTo(afterFirst);
        int firstIndex = dataSource.getConnectionInitSql().indexOf(CITUS_CONNECTION_INIT_SQL);
        assertThat(dataSource.getConnectionInitSql().indexOf(CITUS_CONNECTION_INIT_SQL, firstIndex + 1)).isEqualTo(-1);
    }

    @Test
    void nonMatchingBeanNamePassesThroughUntouched() {
        CitusConnectionInitSqlPostProcessor processor = postProcessor(true);
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setConnectionInitSql(OPERATOR_INIT_SQL);

        Object result = processor.postProcessAfterInitialization(dataSource, "someOtherDataSource");

        assertThat(result).isSameAs(dataSource);
        assertThat(dataSource.getConnectionInitSql()).isEqualTo(OPERATOR_INIT_SQL);
    }

    @Test
    void nonHikariDataSourceBeanPassesThroughUntouched() {
        CitusConnectionInitSqlPostProcessor processor = postProcessor(true);
        Object bean = new Object();

        Object result = processor.postProcessAfterInitialization(bean, DATA_SOURCE_BEAN_NAME);

        assertThat(result).isSameAs(bean);
    }

    @Test
    void citusDisabledIsNoOp() {
        CitusConnectionInitSqlPostProcessor processor = postProcessor(false);
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setConnectionInitSql(OPERATOR_INIT_SQL);

        processor.postProcessAfterInitialization(dataSource, DATA_SOURCE_BEAN_NAME);

        assertThat(dataSource.getConnectionInitSql()).isEqualTo(OPERATOR_INIT_SQL);
    }
}
