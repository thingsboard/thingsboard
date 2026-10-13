// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.service.tenant.exporting;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.MigrationService;
import org.thingsboard.migrator.Table;
import org.thingsboard.migrator.utils.CassandraService;
import org.thingsboard.migrator.utils.Storage;

import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${mode}' == 'TENANT_DATA_EXPORT' and ${export.cassandra.enabled} == true")
@Order(2)
public class CassandraTsKvExporter extends MigrationService {

    private final Storage storage;
    private final CassandraService cassandraService;

    @Value("${export.cassandra.ts.filter.start_ts:}")
    private Long startTs;
    @Value("${export.cassandra.ts.filter.end_ts:}")
    private Long endTs;
    @Value("${export.cassandra.ts.filter.partition_ts:}")
    private Long partitionFilter;
    @Value("${export.cassandra.ts.filter.partition_size_ms:2678400000}")
    private long partitionSizeMs;
    @Value("${export.cassandra.ts.order.ts:false}")
    private boolean orderByTimestamp;

    public static final String TS_KV_TABLE = "ts_kv_cf";
    public static final String TS_KV_PARTITIONS_TABLE = "ts_kv_partitions_cf";
    public static final String TS_KV_FILE = "ts_kv";
    public static final String LATEST_KV = Table.LATEST_KV.getName();

    private Writer writer;

    private boolean isFilterByPartition() {
        return partitionFilter != null && partitionFilter > 0;
    }

    @Override
    protected void start() throws Exception {
        if (isFilterByPartition()) {
            log.info("Cassandra TS filter by partition [{}] is ACTIVE and other filters are IGNORED (startTs, endTs, partitionSizeMs).", partitionFilter);
        } else {
            // Validate time filter once at start
            if (startTs != null && endTs != null && startTs > endTs) {
                log.error("Invalid time range configuration: startTs {} is greater than endTs {}. Aborting Cassandra TS export.", startTs, endTs);
                return;
            }
            if (startTs != null || endTs != null) {
                log.info("Cassandra TS export filter is active. startTs={}, endTs={}, partitionSizeMs={}", startTs, endTs, partitionSizeMs);
            }
        }

        storage.newFile(TS_KV_FILE);
        writer = storage.newWriter(TS_KV_FILE);

        log.info("Starting Cassandra TS export. Latest KV file info: {}", storage.getFileInfo(LATEST_KV));
        storage.readAndProcess(LATEST_KV, latestKvRow -> {
            executor.submit(() -> {
                try {
                    getTsHistoryAndSave(latestKvRow);
                    reportProcessed(LATEST_KV, latestKvRow);
                } catch (Exception e) {
                    log.error("Failed to retrieve timeseries history for {}", latestKvRow, e);
                }
            });
        });
    }

    private void getTsHistoryAndSave(Map<String, Object> latestKvRow) {
        String entityType = (String) latestKvRow.get("table_name");
        UUID entityId = (UUID) latestKvRow.get("entity_id");
        String key = (String) latestKvRow.get("key_name");

        // Use injected timestamp bounds (epoch millis)
        final Long startTs = isFilterByPartition() ? null : this.startTs;
        final Long endTs = isFilterByPartition() ? null : this.endTs;

        List<Long> partitions;
        if (isFilterByPartition()) {
            partitions = List.of(this.partitionFilter);
        } else {
            // Build a partition-bounded query to minimize DB requests
            StringBuilder pQuery = new StringBuilder("SELECT partition FROM " + TS_KV_PARTITIONS_TABLE + " WHERE entity_type = ? AND entity_id = ? AND key = ?");
            List<Object> pArgs = new ArrayList<>();
            pArgs.add(entityType);
            pArgs.add(entityId);
            pArgs.add(key);
            if (startTs != null) {
                pQuery.append(" AND partition >= ?");
                pArgs.add(startTs - partitionSizeMs); // use a partition size window
            }
            if (endTs != null) {
                pQuery.append(" AND partition <= ?");
                pArgs.add(endTs);
            }
            partitions = cassandraService.query(pQuery.toString(), Long.class, pArgs.toArray());
        }
        for (Long partition : partitions) {
            // Safety check to skip out-of-range partitions
            // Effectively ignored when partitionFilter is active
            if (startTs != null && partition < (startTs - partitionSizeMs)) {
                continue;
            }
            if (endTs != null && partition > endTs) {
                continue;
            }

            StringBuilder query = new StringBuilder("SELECT entity_type, entity_id, partition, key, ts, bool_v, str_v, long_v, dbl_v, json_v FROM " + TS_KV_TABLE + " WHERE entity_type = ? AND entity_id = ? AND key = ? AND partition = ?");
            List<Object> args = new ArrayList<>();
            args.add(entityType);
            args.add(entityId);
            args.add(key);
            args.add(partition);

            if (startTs != null) {
                query.append(" AND ts >= ?");
                args.add(startTs);
            }
            if (endTs != null) {
                query.append(" AND ts <= ?");
                args.add(endTs);
            }
            if (orderByTimestamp) {
                query.append(" ORDER BY ts");
            }

            ResultSet rows = cassandraService.query(query.toString(), args.toArray());
            for (Row row : rows) {
                Map<String, Object> data = new LinkedHashMap<>(); //preserve order of columns
                for (ColumnDefinition columnDefinition : row.getColumnDefinitions()) {
                    String column = columnDefinition.getName().toString();
                    Object value = row.getObject(columnDefinition.getName());
                    if (column.endsWith("_v") && value == null) {
                        continue;
                    }
                    data.put(column, value);
                }
                storage.addToFile(writer, data);
                reportProcessed(TS_KV_TABLE, data);
            }
        }
    }

    @Override
    protected void afterFinished() throws Exception {
        finishedProcessing(TS_KV_TABLE);
        finishedProcessing(LATEST_KV);
        writer.close();
    }

}
