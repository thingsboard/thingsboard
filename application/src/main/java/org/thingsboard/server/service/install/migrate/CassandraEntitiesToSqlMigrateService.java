/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.install.migrate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.util.NoSqlAnyDao;
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.bigintColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.booleanColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.doubleColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.enumToIntColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.idColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.jsonColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.stringColumn;

@Service
@Profile("install")
@NoSqlAnyDao
@Slf4j
public class CassandraEntitiesToSqlMigrateService implements EntitiesMigrateService {

    @Autowired
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

    @Autowired
    protected CassandraCluster cluster;

    @Value("${spring.datasource.url}")
    protected String dbUrl;

    @Value("${spring.datasource.username}")
    protected String dbUserName;

    @Value("${spring.datasource.password}")
    protected String dbPassword;

    @Override
    public void migrate() throws Exception {
        log.info("Performing migration of entities data from cassandra to SQL database ...");
        entityDatabaseSchemaService.createDatabaseSchema(false);
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.setAutoCommit(false);
            for (CassandraToSqlTable table: tables) {
                table.migrateToSql(cluster.getSession(), conn);
            }
        } catch (Exception e) {
            log.error("Unexpected error during V-Sensor entities data migration!", e);
            throw e;
        }
        entityDatabaseSchemaService.createDatabaseIndexes();
    }

    private static List<CassandraToSqlTable> tables = Arrays.asList(
       new CassandraToSqlTable("admin_settings",
                idColumn("id"),
                stringColumn("key"),
                stringColumn("json_value")),
        new CassandraToSqlTable("alarm",
                idColumn("id"),
                idColumn("tenant_id"),
                stringColumn("type"),
                idColumn("originator_id"),
                enumToIntColumn("originator_type", EntityType.class),
                stringColumn("severity"),
                stringColumn("status"),
                bigintColumn("start_ts"),
                bigintColumn("end_ts"),
                bigintColumn("ack_ts"),
                bigintColumn("clear_ts"),
                stringColumn("details", "additional_info"),
                booleanColumn("propagate"),
                stringColumn("propagate_relation_types")),
        new CassandraToSqlTable("asset",
                idColumn("id"),
                idColumn("tenant_id"),
                idColumn("customer_id"),
                stringColumn("name"),
                stringColumn("type"),
                stringColumn("label"),
                stringColumn("search_text"),
                stringColumn("additional_info")) {
            @Override
            protected boolean onConstraintViolation(List<CassandraToSqlColumnData[]> batchData,
                                                    CassandraToSqlColumnData[] data, String constraint) {
                if (constraint.equalsIgnoreCase("asset_name_unq_key")) {
                    this.handleUniqueNameViolation(data, "asset");
                    return true;
                }
                return super.onConstraintViolation(batchData, data, constraint);
            }
        },
        new CassandraToSqlTable("audit_log_by_tenant_id", "audit_log",
                idColumn("id"),
                idColumn("tenant_id"),
                idColumn("customer_id"),
                idColumn("entity_id"),
                stringColumn("entity_type"),
                stringColumn("entity_name"),
                idColumn("user_id"),
                stringColumn("user_name"),
                stringColumn("action_type"),
                stringColumn("action_data"),
                stringColumn("action_status"),
                stringColumn("action_failure_details")),
        new CassandraToSqlTable("attributes_kv_cf", "attribute_kv",
                idColumn("entity_id"),
                stringColumn("entity_type"),
                stringColumn("attribute_type"),
                stringColumn("attribute_key"),
                booleanColumn("bool_v", true),
                stringColumn("str_v"),
                bigintColumn("long_v"),
                doubleColumn("dbl_v"),
                jsonColumn("json_v"),
                bigintColumn("last_update_ts")),
        new CassandraToSqlTable("component_descriptor",
                idColumn("id"),
                stringColumn("type"),
                stringColumn("scope"),
                stringColumn("name"),
                stringColumn("search_text"),
                stringColumn("clazz"),
                stringColumn("configuration_descriptor"),
                stringColumn("actions")) {
            @Override
            protected boolean onConstraintViolation(List<CassandraToSqlColumnData[]> batchData,
                                                    CassandraToSqlColumnData[] data, String constraint) {
                if (constraint.equalsIgnoreCase("component_descriptor_clazz_key")) {
                    String clazz = this.getColumnData(data, "clazz").getValue();
                    log.warn("Found component_descriptor record with duplicate clazz [{}]. Record will be ignored!", clazz);
                    this.ignoreRecord(batchData, data);
                    return true;
                }
                return super.onConstraintViolation(batchData, data, constraint);
            }
        },
        new CassandraToSqlTable("customer",
                idColumn("id"),
                idColumn("tenant_id"),
                stringColumn("title"),
                stringColumn("search_text"),
                stringColumn("country"),
                stringColumn("state"),
                stringColumn("city"),
                stringColumn("address"),
                stringColumn("address2"),
                stringColumn("zip"),
                stringColumn("phone"),
                stringColumn("email"),
                stringColumn("additional_info")),
        new CassandraToSqlTable("dashboard",
                idColumn("id"),
                idColumn("tenant_id"),
                stringColumn("title"),
                stringColumn("search_text"),
                stringColumn("assigned_customers"),
                stringColumn("configuration")),
        new CassandraToSqlTable("device",
                idColumn("id"),
                idColumn("tenant_id"),
                idColumn("customer_id"),
                stringColumn("name"),
                stringColumn("type"),
                stringColumn("label"),
                stringColumn("search_text"),
                stringColumn("additional_info")) {
            @Override
            protected boolean onConstraintViolation(List<CassandraToSqlColumnData[]> batchData,
                                                    CassandraToSqlColumnData[] data, String constraint) {
                if (constraint.equalsIgnoreCase("device_name_unq_key")) {
                    this.handleUniqueNameViolation(data, "device");
                    return true;
                }
                return super.onConstraintViolation(batchData, data, constraint);
            }
        },
        new CassandraToSqlTable("device_credentials",
                idColumn("id"),
                idColumn("device_id"),
                stringColumn("credentials_type"),
                stringColumn("credentials_id"),
                stringColumn("credentials_value")),
        new CassandraToSqlTable("event",
                idColumn("id"),
                idColumn("tenant_id"),
                idColumn("entity_id"),
                stringColumn("entity_type"),
                stringColumn("event_type"),
                stringColumn("event_uid"),
                stringColumn("body"),
                new CassandraToSqlEventTsColumn()),
        new CassandraToSqlTable("relation",
                idColumn("from_id"),
                stringColumn("from_type"),
                idColumn("to_id"),
                stringColumn("to_type"),
                stringColumn("relation_type_group"),
                stringColumn("relation_type"),
                stringColumn("additional_info")),
        new CassandraToSqlTable("user", "tb_user",
                idColumn("id"),
                idColumn("tenant_id"),
                idColumn("customer_id"),
                stringColumn("email"),
                stringColumn("search_text"),
                stringColumn("authority"),
                stringColumn("first_name"),
                stringColumn("last_name"),
                stringColumn("additional_info")) {
            @Override
            protected boolean onConstraintViolation(List<CassandraToSqlColumnData[]> batchData,
                                                    CassandraToSqlColumnData[] data, String constraint) {
                if (constraint.equalsIgnoreCase("tb_user_email_key")) {
                    this.handleUniqueEmailViolation(data);
                    return true;
                }
                return super.onConstraintViolation(batchData, data, constraint);
            }
        },
        new CassandraToSqlTable("tenant",
                idColumn("id"),
                stringColumn("title"),
                stringColumn("search_text"),
                stringColumn("region"),
                stringColumn("country"),
                stringColumn("state"),
                stringColumn("city"),
                stringColumn("address"),
                stringColumn("address2"),
                stringColumn("zip"),
                stringColumn("phone"),
                stringColumn("email"),
                stringColumn("additional_info"),
                booleanColumn("isolated_tb_core"),
                booleanColumn("isolated_tb_rule_engine")),
        new CassandraToSqlTable("user_credentials",
                idColumn("id"),
                idColumn("user_id"),
                booleanColumn("enabled"),
                stringColumn("password"),
                stringColumn("activate_token"),
                stringColumn("reset_token")) {
            @Override
            protected boolean onConstraintViolation(List<CassandraToSqlColumnData[]> batchData,
                                                    CassandraToSqlColumnData[] data, String constraint) {
                if (constraint.equalsIgnoreCase("user_credentials_user_id_key")) {
                    String id = UUIDConverter.fromString(this.getColumnData(data, "id").getValue()).toString();
                    log.warn("Found user credentials record with duplicate user_id [id:[{}]]. Record will be ignored!", id);
                    this.ignoreRecord(batchData, data);
                    return true;
                }
                return super.onConstraintViolation(batchData, data, constraint);
            }
        },
        new CassandraToSqlTable("widget_type",
                idColumn("id"),
                idColumn("tenant_id"),
                stringColumn("bundle_alias"),
                stringColumn("alias"),
                stringColumn("name"),
                stringColumn("descriptor")),
        new CassandraToSqlTable("widgets_bundle",
                idColumn("id"),
                idColumn("tenant_id"),
                stringColumn("alias"),
                stringColumn("title"),
                stringColumn("search_text")),
        new CassandraToSqlTable("rule_chain",
                idColumn("id"),
                idColumn("tenant_id"),
                stringColumn("name"),
                stringColumn("search_text"),
                idColumn("first_rule_node_id"),
                booleanColumn("root"),
                booleanColumn("debug_mode"),
                stringColumn("configuration"),
                stringColumn("additional_info")),
        new CassandraToSqlTable("rule_node",
                idColumn("id"),
                idColumn("rule_chain_id"),
                stringColumn("type"),
                stringColumn("name"),
                booleanColumn("debug_mode"),
                stringColumn("search_text"),
                stringColumn("configuration"),
                stringColumn("additional_info")),
        new CassandraToSqlTable("entity_view",
                idColumn("id"),
                idColumn("tenant_id"),
                idColumn("customer_id"),
                idColumn("entity_id"),
                stringColumn("entity_type"),
                stringColumn("name"),
                stringColumn("type"),
                stringColumn("keys"),
                bigintColumn("start_ts"),
                bigintColumn("end_ts"),
                stringColumn("search_text"),
                stringColumn("additional_info"))
    );
}
