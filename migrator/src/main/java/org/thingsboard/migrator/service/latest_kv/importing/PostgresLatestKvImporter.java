// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.service.latest_kv.importing;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.migrator.MigrationService;
import org.thingsboard.migrator.service.latest_kv.exporting.CassandraLatestKvExporter;
import org.thingsboard.migrator.utils.Storage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${mode}' == 'POSTGRES_LATEST_KV_IMPORT'")
@Slf4j
public class PostgresLatestKvImporter extends MigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Storage storage;

    @Value("${import.postgres.delay_between_queries}")
    private int delayBetweenQueries;
    @Value("${import.postgres.ignore_conflicts}")
    private boolean ignoreConflicts;

    private static final String LATEST_KV_TABLE = "ts_kv_latest";

    private Map<String, String> columns;

    @Override
    protected void start() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                storage.readAndProcess(CassandraLatestKvExporter.LATEST_KV_FILE, row -> {
                    saveRow(row);
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void saveRow(Map<String, Object> row) {
        prepareRow(row);

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
            }
            String valueType = columns.get(column);
            valuesStatement += "::" + valueType;
        }

        String query = format("INSERT INTO " + LATEST_KV_TABLE + " (%s) VALUES (%s)", columnsStatement, valuesStatement);
        if (ignoreConflicts) {
            query += " ON CONFLICT DO NOTHING";
        }
        jdbcTemplate.update(query, row.values().toArray());
        reportProcessed(LATEST_KV_TABLE, row);
        try {
            TimeUnit.MILLISECONDS.sleep(delayBetweenQueries);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareRow(Map<String, Object> row) {
        String keyName = (String) row.remove("key_name");
        Integer keyId = jdbcTemplate.queryForList("SELECT key_id FROM ts_kv_dictionary WHERE key = ?", Integer.class, keyName).stream().findFirst().orElse(null);
        if (keyId == null) {
            jdbcTemplate.update("INSERT INTO ts_kv_dictionary (key) VALUES (?)", keyName);
            keyId = jdbcTemplate.queryForObject("SELECT key_id FROM ts_kv_dictionary WHERE key = ?", Integer.class, keyName);
            log.info("Inserted key '{}' into ts_kv_dictionary (new key id: {})", keyName, keyId);
        }
        row.put("key", keyId);

        if (columns == null) {
            columns = jdbcTemplate.queryForList("SELECT column_name, udt_name FROM information_schema.columns " +
                            "WHERE table_schema = 'public' AND table_name = '" + LATEST_KV_TABLE + "'").stream()
                    .collect(Collectors.toMap(vals -> vals.get("column_name").toString(), vals -> vals.get("udt_name").toString()));
        }
        row.keySet().removeIf(column -> !columns.containsKey(column));
    }

    @Override
    protected void afterFinished() throws Exception {
        finishedProcessing(LATEST_KV_TABLE);
    }

}
