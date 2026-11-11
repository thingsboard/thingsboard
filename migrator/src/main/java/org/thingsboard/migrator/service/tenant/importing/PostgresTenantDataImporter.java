// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.service.tenant.importing;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.migrator.MigrationService;
import org.thingsboard.migrator.Table;
import org.thingsboard.migrator.utils.PostgresService;
import org.thingsboard.migrator.utils.PostgresService.Blob;
import org.thingsboard.migrator.utils.SqlPartitionService;
import org.thingsboard.migrator.utils.Storage;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${mode}' == 'TENANT_DATA_IMPORT' and ${import.postgres.enabled} == true")
@Order(1)
@Slf4j
public class PostgresTenantDataImporter extends MigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Storage storage;
    private final SqlPartitionService partitionService;
    private final PostgresService postgresService;

    @Value("${skipped_tables}")
    private Set<Table> skippedTables;
    @Value("${import.postgres.delay_between_queries}")
    private int delayBetweenQueries;

    @Value("${import.postgres.enable_partition_creation}")
    private boolean enablePartitionCreation;
    @Value("${import.postgres.update_tenant_profile}")
    private boolean updateTenantProfile;
    @Value("${import.postgres.update_ts_kv_dictionary}")
    private boolean updateKeyDictionary;
    @Value("${import.postgres.resolve_unknown_roles}")
    private boolean resolveUnknownRoles;

    private final Map<Table, Map<String, String>> columns = new HashMap<>();

    @Override
    protected void start() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            prepare();
            for (Table table : Table.values()) {
                if (skippedTables.contains(table)) {
                    continue;
                }
                importTableData(table);
            }
            tearDown();
        });
    }

    @SneakyThrows
    private void importTableData(Table table) {
        log.info("Importing table {} (file info: {})", table.getName(), storage.getFileInfo(table.getName()));
        storage.readAndProcess(table.getName(), row -> {
            saveRow(table, row);
        });
        finishedProcessing(table.getName());
    }

    private void saveRow(Table table, Map<String, Object> row) {
        row = prepareRow(table, row);
        if (table.isPartitioned() && enablePartitionCreation) {
            partitionService.createPartition(table, row);
        }

        String columnsStatement = "";
        String valuesStatement = "";
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String column = entry.getKey();
            Object value = entry.getValue();

            if (!columnsStatement.isEmpty()) {
                columnsStatement += ",";
            }
            columnsStatement += column;

            if (!valuesStatement.isEmpty()) {
                valuesStatement += ",";
            }
            valuesStatement += "?";
            if (value instanceof JsonNode) {
                entry.setValue(value.toString());
            } else if (value instanceof String[]) {
                try {
                    entry.setValue(jdbcTemplate.getDataSource().getConnection().createArrayOf("text", (String[]) value));
                    continue;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            String valueType = columns.get(table).get(column);
            valuesStatement += "::" + valueType;
        }

        String query = format("INSERT INTO %s (%s) VALUES (%s)", table.getName(), columnsStatement, valuesStatement);
        jdbcTemplate.update(query, row.values().toArray());
        reportProcessed(table.getName(), row);
        try {
            TimeUnit.MILLISECONDS.sleep(delayBetweenQueries);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> prepareRow(Table table, Map<String, Object> row) {
        if (table == Table.TENANT) {
            if (updateTenantProfile) {
                UUID defaultTenantProfile = jdbcTemplate.queryForList("SELECT id FROM tenant_profile WHERE is_default = TRUE", UUID.class).get(0);
                row = new LinkedHashMap<>(row);
                row.put("tenant_profile_id", defaultTenantProfile);
            }
        } else if (table == Table.LATEST_KV || table == Table.ATTRIBUTE) {
            String keyName = (String) row.remove("key_name");
            if (updateKeyDictionary) {
                Integer keyId = jdbcTemplate.queryForList("SELECT key_id FROM key_dictionary WHERE key = ?", Integer.class, keyName).stream().findFirst().orElse(null);
                if (keyId == null) {
                    keyId = jdbcTemplate.queryForObject("INSERT INTO key_dictionary (key) VALUES (?) RETURNING key_id", Integer.class, keyName);
                }
                if (table == Table.LATEST_KV) {
                    row.put("key", keyId);
                } else {
                    row.put("attribute_key", keyId);
                }
            }
        } else if (table == Table.GROUP_PERMISSION) {
            UUID roleId = (UUID) row.get("role_id");
            String roleName = (String) row.remove("role_name");
            Boolean roleExists = jdbcTemplate.queryForObject("SELECT EXISTS (SELECT * FROM role WHERE id = ?)", Boolean.class, roleId);
            if (!roleExists) {
                if (!resolveUnknownRoles) {
                    throw new IllegalArgumentException("Role with id " + roleId + " not found");
                }
                // happens when system role is used (e.g. 'Tenant Administrators')
                log.info("Role for id {} does not exist. Finding by name {}", roleId, roleName);
                Map<String, Object> role = jdbcTemplate.queryForList("SELECT * FROM role WHERE name = ?", roleName).stream()
                        .findFirst().orElse(null);
                if (role == null) {
                    throw new IllegalArgumentException("Role not found for name " + roleName);
                }
                row.put("role_id", role.get("id"));
            }
        }
        row.replaceAll((key, value) -> {
            if (value instanceof Blob blob) {
                return postgresService.saveBlob(blob);
            }
            return value;
        });
        row.remove("table_name");
        Object version = row.get("version");
        if (version instanceof Number) {
            row.remove("version");
        }

        Map<String, String> existingColumns = columns.computeIfAbsent(table, t -> {
            return jdbcTemplate.queryForList("SELECT column_name, udt_name FROM information_schema.columns " +
                                             "WHERE table_schema = 'public' AND table_name = '" + table.getName() + "'").stream()
                    .collect(Collectors.toMap(vals -> vals.get("column_name").toString(), vals -> vals.get("udt_name").toString()));
        });
        row.keySet().removeIf(column -> {
            boolean unknownColumn = !existingColumns.containsKey(column);
            if (unknownColumn) {
                log.warn("Skipping unknown column {} for table {}", column, table.getName());
            }
            return unknownColumn;
        });
        return row;
    }

    private void prepare() {
        jdbcTemplate.execute("ALTER TABLE ota_package DROP CONSTRAINT IF EXISTS fk_device_profile_ota_package");
        log.info("Temporarily dropped fk_device_profile_ota_package constraint");
    }

    private void tearDown() {
        jdbcTemplate.execute("ALTER TABLE ota_package ADD CONSTRAINT fk_device_profile_ota_package FOREIGN KEY (device_profile_id) REFERENCES device_profile (id) ON DELETE CASCADE");
        log.info("Created fk_device_profile_ota_package constraint");
    }

}
