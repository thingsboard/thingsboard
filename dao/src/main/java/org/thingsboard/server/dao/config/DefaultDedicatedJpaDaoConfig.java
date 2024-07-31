package org.thingsboard.server.dao.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@ConditionalOnProperty(value = "spring.datasource.dedicated.enabled", havingValue = "false", matchIfMissing = true)
@Configuration
@EnableJpaRepositories(value = {"org.thingsboard.server.dao.sql.event", "org.thingsboard.server.dao.sql.audit"}, bootstrapMode = BootstrapMode.LAZY)
public class DefaultDedicatedJpaDaoConfig {

    @Bean
    public JdbcTemplate dedicatedJdbcTemplate(@Qualifier("jdbcTemplate") JdbcTemplate defaultJdbcTemplate) {
        return defaultJdbcTemplate;
    }

    @Bean
    public TransactionTemplate dedicatedTransactionTemplate(@Qualifier("transactionTemplate") TransactionTemplate defaultTransactionTemplate) {
        return defaultTransactionTemplate;
    }

}
