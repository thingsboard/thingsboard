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

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;
import org.thingsboard.server.dao.model.sql.ErrorEventEntity;
import org.thingsboard.server.dao.model.sql.LifecycleEventEntity;
import org.thingsboard.server.dao.model.sql.RuleChainDebugEventEntity;
import org.thingsboard.server.dao.model.sql.RuleNodeDebugEventEntity;
import org.thingsboard.server.dao.model.sql.StatisticsEventEntity;

import javax.sql.DataSource;
import java.util.Objects;

/*
 * To make entity use a dedicated datasource:
 * - add its JpaRepository to exclusions list in @EnableJpaRepositories in JpaDaoConfig
 * - add the package of this JpaRepository to @EnableJpaRepositories in DefaultDedicatedJpaDaoConfig
 * - add the package of this JpaRepository to @EnableJpaRepositories in DedicatedJpaDaoConfig
 * - add the entity class to packages list in dedicatedEntityManagerFactory in DedicatedJpaDaoConfig
 * */
@DedicatedDataSource
@Configuration
@EnableJpaRepositories(value = {"org.thingsboard.server.dao.sql.event", "org.thingsboard.server.dao.sql.audit"},
        bootstrapMode = BootstrapMode.LAZY,
        entityManagerFactoryRef = "dedicatedEntityManagerFactory", transactionManagerRef = "dedicatedTransactionManager")
public class DedicatedJpaDaoConfig {

    public static final String DEDICATED_PERSISTENCE_UNIT = "dedicated";
    public static final String DEDICATED_TRANSACTION_MANAGER = DEDICATED_PERSISTENCE_UNIT + "TransactionManager";
    public static final String DEDICATED_TRANSACTION_TEMPLATE = DEDICATED_PERSISTENCE_UNIT + "TransactionTemplate";
    public static final String DEDICATED_JDBC_TEMPLATE = DEDICATED_PERSISTENCE_UNIT + "JdbcTemplate";

    @Bean
    @ConfigurationProperties("spring.datasource.dedicated")
    public DataSourceProperties dedicatedDataSourceProperties() {
        return new DataSourceProperties();
    }

    @ConfigurationProperties(prefix = "spring.datasource.dedicated.hikari")
    @Bean
    public DataSource dedicatedDataSource(@Qualifier("dedicatedDataSourceProperties") DataSourceProperties dedicatedDataSourceProperties) {
        return dedicatedDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean dedicatedEntityManagerFactory(@Qualifier("dedicatedDataSource") DataSource dedicatedDataSource,
                                                                                EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dedicatedDataSource)
                .packages(LifecycleEventEntity.class, StatisticsEventEntity.class, ErrorEventEntity.class, RuleNodeDebugEventEntity.class, RuleChainDebugEventEntity.class, AuditLogEntity.class)
                .persistenceUnit(DEDICATED_PERSISTENCE_UNIT)
                .build();
    }

    @Bean(DEDICATED_TRANSACTION_MANAGER)
    public JpaTransactionManager dedicatedTransactionManager(@Qualifier("dedicatedEntityManagerFactory") LocalContainerEntityManagerFactoryBean dedicatedEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(dedicatedEntityManagerFactory.getObject()));
    }

    @Bean(DEDICATED_TRANSACTION_TEMPLATE)
    public TransactionTemplate dedicatedTransactionTemplate(@Qualifier(DEDICATED_TRANSACTION_MANAGER) JpaTransactionManager dedicatedTransactionManager) {
        return new TransactionTemplate(dedicatedTransactionManager);
    }

    @Bean(DEDICATED_JDBC_TEMPLATE)
    public JdbcTemplate dedicatedJdbcTemplate(@Qualifier("dedicatedDataSource") DataSource dedicatedDataSource) {
        return new JdbcTemplate(dedicatedDataSource);
    }

}
