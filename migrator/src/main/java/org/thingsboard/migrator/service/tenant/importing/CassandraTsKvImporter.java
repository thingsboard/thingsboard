// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.service.tenant.importing;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.MigrationService;
import org.thingsboard.migrator.service.tenant.exporting.CassandraTsKvExporter;
import org.thingsboard.migrator.utils.CassandraService;
import org.thingsboard.migrator.utils.Storage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.thingsboard.migrator.service.tenant.exporting.CassandraTsKvExporter.TS_KV_PARTITIONS_TABLE;
import static org.thingsboard.migrator.service.tenant.exporting.CassandraTsKvExporter.TS_KV_TABLE;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${mode}' == 'TENANT_DATA_IMPORT' and ${import.cassandra.enabled} == true")
@Order(2)
public class CassandraTsKvImporter extends MigrationService {

    private final Storage storage;
    private final CassandraService cassandraService;

    private final Map<PartitionKey, Set<Long>> partitions = new WeakHashMap<>();

    @Value("${import.cassandra.ttl_days:730}")
    private int tsKvTtlDays;

    @Value("${import.cassandra.skip_lines:0}")
    private long tsKvSkipLines;

    @Override
    protected void start() throws Exception {
        log.info("Starting CassandraTsKvImporter...");
        log.info("Reporting info for file: {}", storage.getFileInfo(CassandraTsKvExporter.TS_KV_FILE));
        if (tsKvSkipLines > 0) {
            log.info("Skipping first {} lines. Initial processed counter will start from {}...", tsKvSkipLines, tsKvSkipLines);
            reportProcessed(TS_KV_TABLE, tsKvSkipLines, null);
        }
        storage.readAndProcess(CassandraTsKvExporter.TS_KV_FILE, tsKvSkipLines, row -> {
            try {
                saveTsKv(row);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save row: " + row, e);
            }
        });
    }

    private void saveTsKv(Map<String, Object> row) {
        String entityType = (String) row.get("entity_type");
        UUID entityId = (UUID) row.get("entity_id");
        String key = (String) row.get("key");
        PartitionKey partitionKey = PartitionKey.of(entityType, entityId, key);
        Long partition = ((Number) row.get("partition")).longValue();
        row.put("partition", partition);
        long ttl = TimeUnit.DAYS.toSeconds(tsKvTtlDays);
        boolean newPartition = partitions.computeIfAbsent(partitionKey, k -> new HashSet<>()).add(partition);
        if (newPartition) {
            String query = "INSERT INTO " + TS_KV_PARTITIONS_TABLE + " (entity_type, entity_id, key, partition) VALUES (?, ?, ?, ?)";
            if (ttl > 0) {
                query += " USING TTL " + ttl;
            }
            String finalQuery = query;
            executor.submit(() -> {
                cassandraService.execute(finalQuery, entityType, entityId, key, partition);
            });
        }

        Object longV = row.get("long_v");
        if (longV != null && !(longV instanceof Long)) {
            row.put("long_v", ((Number) longV).longValue());
        }
        Object dblV = row.get("dbl_v");
        if (dblV != null && !(dblV instanceof Double)) {
            row.put("dbl_v", ((Number) dblV).doubleValue());
        }
        Number ts = (Number) row.get("ts");
        ts = ts.longValue();
        row.put("ts", ts);

        String columnsStmt = String.join(", ", row.keySet());
        String valuesStmt = StringUtils.removeEnd("?,".repeat(row.size()), ",");
        String query = format("INSERT INTO " + TS_KV_TABLE + " (%s) VALUES (%s)", columnsStmt, valuesStmt);
        if (ttl > 0) {
            query += " USING TTL " + ttl;
        }
        String finalQuery = query;
        executor.submit(() -> {
            cassandraService.execute(finalQuery, row.values().toArray());
            reportProcessed(TS_KV_TABLE, row);
        });
    }

    @Override
    protected void afterFinished() throws Exception {
        finishedProcessing(TS_KV_TABLE);
    }

    @Data(staticConstructor = "of")
    private static class PartitionKey {
        private final String entityType;
        private final UUID entityId;
        private final String key;
    }

}
