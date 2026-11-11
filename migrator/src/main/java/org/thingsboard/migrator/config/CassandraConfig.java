// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Disables Cassandra autoconfiguration if Cassandra export/import is disabled
 * */
@Configuration
@EnableAutoConfiguration(exclude = {CassandraDataAutoConfiguration.class, CassandraAutoConfiguration.class})
@ConditionalOnExpression("('${mode}' == 'TENANT_DATA_EXPORT' && ${export.cassandra.enabled} == false) || ('${mode}' == 'TENANT_DATA_IMPORT' and ${import.cassandra.enabled} == false) || ('${mode}' == 'CASSANDRA_LATEST_KV_EXPORT')")
public class CassandraConfig {
}
