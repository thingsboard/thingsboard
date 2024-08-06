/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.thingsboard.server.dao.config.DedicatedJpaDaoConfig.DEDICATED_JDBC_TEMPLATE;
import static org.thingsboard.server.dao.config.DedicatedJpaDaoConfig.DEDICATED_TRANSACTION_MANAGER;
import static org.thingsboard.server.dao.config.DedicatedJpaDaoConfig.DEDICATED_TRANSACTION_TEMPLATE;

@ConditionalOnProperty(value = "spring.datasource.dedicated.enabled", havingValue = "false", matchIfMissing = true)
@Configuration
@EnableJpaRepositories(value = {"org.thingsboard.server.dao.sql.event", "org.thingsboard.server.dao.sql.audit"}, bootstrapMode = BootstrapMode.LAZY)
public class DefaultDedicatedJpaDaoConfig {

    @Bean(DEDICATED_JDBC_TEMPLATE)
    public JdbcTemplate dedicatedJdbcTemplate(@Qualifier("jdbcTemplate") JdbcTemplate defaultJdbcTemplate) {
        return defaultJdbcTemplate;
    }

    @Bean(DEDICATED_TRANSACTION_MANAGER)
    public JpaTransactionManager dedicatedTransactionManager(@Qualifier("transactionManager") JpaTransactionManager defaultTransactionManager) {
        return defaultTransactionManager;
    }

    @Bean(DEDICATED_TRANSACTION_TEMPLATE)
    public TransactionTemplate dedicatedTransactionTemplate(@Qualifier("transactionTemplate") TransactionTemplate defaultTransactionTemplate) {
        return defaultTransactionTemplate;
    }

}
