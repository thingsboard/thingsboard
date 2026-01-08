/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.dao.model.sql.CalculatedFieldDebugEventEntity;
import org.thingsboard.server.dao.model.sql.ErrorEventEntity;
import org.thingsboard.server.dao.model.sql.LifecycleEventEntity;
import org.thingsboard.server.dao.model.sql.RuleChainDebugEventEntity;
import org.thingsboard.server.dao.model.sql.RuleNodeDebugEventEntity;
import org.thingsboard.server.dao.model.sql.StatisticsEventEntity;

import javax.sql.DataSource;
import java.util.Objects;

@DedicatedEventsDataSource
@Configuration
@EnableJpaRepositories(value = {"org.thingsboard.server.dao.sql.event", "org.thingsboard.server.dao.sql.audit"},
        bootstrapMode = BootstrapMode.LAZY,
        entityManagerFactoryRef = "eventsEntityManagerFactory", transactionManagerRef = "eventsTransactionManager")
public class DedicatedEventsJpaDaoConfig {

    public static final String EVENTS_PERSISTENCE_UNIT = "events";
    public static final String EVENTS_DATA_SOURCE = EVENTS_PERSISTENCE_UNIT + "DataSource";
    public static final String EVENTS_TRANSACTION_MANAGER = EVENTS_PERSISTENCE_UNIT + "TransactionManager";
    public static final String EVENTS_TRANSACTION_TEMPLATE = EVENTS_PERSISTENCE_UNIT + "TransactionTemplate";
    public static final String EVENTS_JDBC_TEMPLATE = EVENTS_PERSISTENCE_UNIT + "JdbcTemplate";

    @Bean
    @ConfigurationProperties("spring.datasource.events")
    public DataSourceProperties eventsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @ConfigurationProperties(prefix = "spring.datasource.events.hikari")
    @Bean(EVENTS_DATA_SOURCE)
    public DataSource eventsDataSource(@Qualifier("eventsDataSourceProperties") DataSourceProperties eventsDataSourceProperties) {
        return eventsDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean eventsEntityManagerFactory(@Qualifier(EVENTS_DATA_SOURCE) DataSource eventsDataSource,
                                                                             EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(eventsDataSource)
                .packages(LifecycleEventEntity.class, StatisticsEventEntity.class, ErrorEventEntity.class, RuleNodeDebugEventEntity.class, RuleChainDebugEventEntity.class, AuditLogEntity.class, CalculatedFieldDebugEventEntity.class)
                .persistenceUnit(EVENTS_PERSISTENCE_UNIT)
                .build();
    }

    @Bean(EVENTS_TRANSACTION_MANAGER)
    public JpaTransactionManager eventsTransactionManager(@Qualifier("eventsEntityManagerFactory") LocalContainerEntityManagerFactoryBean eventsEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(eventsEntityManagerFactory.getObject()));
    }

    @Bean(EVENTS_TRANSACTION_TEMPLATE)
    public TransactionTemplate eventsTransactionTemplate(@Qualifier(EVENTS_TRANSACTION_MANAGER) JpaTransactionManager eventsTransactionManager) {
        return new TransactionTemplate(eventsTransactionManager);
    }

    @Bean(EVENTS_JDBC_TEMPLATE)
    public JdbcTemplate eventsJdbcTemplate(@Qualifier(EVENTS_DATA_SOURCE) DataSource eventsDataSource) {
        return new JdbcTemplate(eventsDataSource);
    }

}
