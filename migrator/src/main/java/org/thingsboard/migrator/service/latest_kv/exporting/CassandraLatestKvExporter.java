// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.service.latest_kv.exporting;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.migrator.MigrationService;
import org.thingsboard.migrator.utils.CassandraService;
import org.thingsboard.migrator.utils.Storage;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${mode}' == 'CASSANDRA_LATEST_KV_EXPORT'")
public class CassandraLatestKvExporter extends MigrationService {

    private final CassandraService cassandraService;
    private final Storage storage;

    private static final String LATEST_KV_TABLE = "ts_kv_latest_cf";
    public static final String LATEST_KV_FILE = "latest_kv";

    @Override
    protected void start() throws Exception {
        storage.newFile(LATEST_KV_FILE);
        try (Writer writer = storage.newWriter(LATEST_KV_FILE)) {
            String query = "SELECT * FROM " + LATEST_KV_TABLE;
            ResultSet rows = cassandraService.query(query);
            for (Row row : rows) {
                Map<String, Object> data = new HashMap<>();
                for (ColumnDefinition columnDefinition : row.getColumnDefinitions()) {
                    String column = columnDefinition.getName().toString();
                    Object value = row.getObject(columnDefinition.getName());
                    if (column.endsWith("_v") && value == null) {
                        continue;
                    }
                    if (column.equals("key")) {
                        column = "key_name";
                    }
                    data.put(column, value);
                }
                storage.addToFile(writer, data);
                reportProcessed(LATEST_KV_TABLE, data);
            }
        }
    }

    @Override
    protected void afterFinished() throws Exception {
        finishedProcessing(LATEST_KV_TABLE);
    }

}
