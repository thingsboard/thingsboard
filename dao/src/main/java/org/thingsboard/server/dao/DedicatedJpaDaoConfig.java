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
package org.thingsboard.server.dao;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import org.thingsboard.server.dao.model.sql.ErrorEventEntity;
import org.thingsboard.server.dao.model.sql.LifecycleEventEntity;
import org.thingsboard.server.dao.model.sql.RuleChainDebugEventEntity;
import org.thingsboard.server.dao.model.sql.RuleNodeDebugEventEntity;
import org.thingsboard.server.dao.model.sql.StatisticsEventEntity;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@EnableJpaRepositories(value = "org.thingsboard.server.dao.sql.event", bootstrapMode = BootstrapMode.LAZY,
        entityManagerFactoryRef = "dedicatedEntityManagerFactory", transactionManagerRef = "dedicatedTransactionManager")
public class DedicatedJpaDaoConfig {

    @Value("${spring.datasource.dedicated.enabled:false}")
    private boolean dedicatedDataSourceEnabled;

    @Bean
    @ConfigurationProperties("spring.datasource.dedicated")
    public DataSourceProperties dedicatedDataSourceProperties() {
        if (dedicatedDataSourceEnabled) {
            return new DataSourceProperties();
        } else {
            return null;
        }
    }

    @ConfigurationProperties(prefix = "spring.datasource.dedicated.hikari")
    @Bean
    public DataSource dedicatedDataSource(@Qualifier("dedicatedDataSourceProperties") DataSourceProperties dedicatedDataSourceProperties) {
        if (dedicatedDataSourceEnabled) {
            return dedicatedDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        } else {
            return null;
        }
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean dedicatedEntityManagerFactory(@Qualifier("dedicatedDataSource") DataSource dedicatedDataSource,
                                                                                @Qualifier("dataSource") DataSource defaultDataSource,
                                                                                EntityManagerFactoryBuilder builder) {
        if (dedicatedDataSourceEnabled) {
            return builder
                    .dataSource(dedicatedDataSource)
                    .packages(LifecycleEventEntity.class, StatisticsEventEntity.class, ErrorEventEntity.class, RuleNodeDebugEventEntity.class, RuleChainDebugEventEntity.class)
                    .persistenceUnit("dedicated")
                    .build();
        } else {
            return null;
        }
    }

    @Bean
    public JpaTransactionManager dedicatedTransactionManager(@Qualifier("dedicatedEntityManagerFactory") LocalContainerEntityManagerFactoryBean dedicatedEntityManagerFactory) {
        if (dedicatedDataSourceEnabled) {
            return new JpaTransactionManager(Objects.requireNonNull(dedicatedEntityManagerFactory.getObject()));
        } else {
            return null;
        }
    }

    @Bean
    public TransactionTemplate dedicatedTransactionTemplate(@Qualifier("dedicatedTransactionManager") JpaTransactionManager dedicatedTransactionManager) {
        if (dedicatedDataSourceEnabled) {
            return new TransactionTemplate(dedicatedTransactionManager);
        } else {
            return null;
        }
    }

    @Bean
    public JdbcTemplate dedicatedJdbcTemplate(@Qualifier("dedicatedDataSource") DataSource dedicatedDataSource) {
        if (dedicatedDataSourceEnabled) {
            return new JdbcTemplate(dedicatedDataSource);
        } else {
            return null;
        }
    }

}
