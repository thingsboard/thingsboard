// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.service.tenant.exporting;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.MigrationService;
import org.thingsboard.migrator.Table;
import org.thingsboard.migrator.utils.PostgresService;
import org.thingsboard.migrator.utils.SqlPartitionService;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${mode}' == 'TENANT_DATA_EXPORT' and ${export.postgres.enabled} == true")
@Order(1)
public class PostgresTenantDataExporter extends MigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final SqlPartitionService partitionService;
    private final PostgresService postgresService;

    @Value("${export.tenant_id}")
    private UUID exportedTenantId;
    @Value("${export.postgres.batch_size}")
    private int batchSize;
    @Value("${export.postgres.delay_between_queries}")
    private int delayBetweenQueries;
    @Value("${skipped_tables}")
    private Set<Table> skippedTables;

    private static final Set<Table> relatedTables = Set.of(Table.RELATION, Table.ATTRIBUTE, Table.LATEST_KV);

    private final Map<Table, Writer> writers = new HashMap<>();

    @Override
    protected void start() throws Exception {
        for (Table table : relatedTables) {
            if (skippedTables.contains(table)) {
                continue;
            }
            storage.newFile(table.getName());
        }
        for (Table table : Table.values()) {
            if (skippedTables.contains(table) || relatedTables.contains(table)) {
                continue;
            }
            exportTableData(table, exportedTenantId);
            finishedProcessing(table.getName());
        }
        for (Table relatedTable : relatedTables) {
            finishedProcessing(relatedTable.getName());
        }

        for (Writer writer : writers.values()) {
            writer.close();
        }
    }

    private void exportTableData(Table table, UUID tenantId) throws IOException {
        storage.newFile(table.getName());
        String query;
        if (table.getCustomSelect() != null) {
            query = table.getCustomSelect().apply(tenantId);
        } else {
            query = format("SELECT * FROM %s WHERE ", table.getName());
        }

        if (table.getReference() == null) {
            query += format("%s.%s = '%s'", table.getName(), table.getTenantIdColumn(), tenantId);
        } else {
            Pair<String, List<Table>> reference = table.getReference();
            String referencingColumn = reference.getKey();
            List<Table> referencedTables = reference.getValue();

            for (Table referencedTable : referencedTables) {
                if (referencedTable.getReference() == null) {
                    query += format(" %s IN (SELECT %s.id FROM %s WHERE %s.%s = '%s') OR",
                            referencingColumn, referencedTable.getName(), referencedTable.getName(),
                            referencedTable.getName(), referencedTable.getTenantIdColumn(), tenantId);
                } else {
                    Pair<String, List<Table>> anotherReference = referencedTable.getReference();
                    String column = anotherReference.getKey();
                    List<Table> tables = anotherReference.getValue();
                    for (Table anotherReferencedTable : tables) {
                        query += format(" %s IN (SELECT %s.id FROM %s INNER JOIN %s ON %s.%s = %s.id WHERE %s.%s = '%s') OR",
                                referencingColumn, referencedTable.getName(), referencedTable.getName(),
                                anotherReferencedTable.getName(), referencedTable.getName(), column, anotherReferencedTable.getName(),
                                anotherReferencedTable.getName(), anotherReferencedTable.getTenantIdColumn(), tenantId);
                    }
                }
            }
            query = StringUtils.removeEnd(query, "OR");
        }
        query += " ORDER BY " + String.join(", ", table.getSortColumns());

        queryAndSave(table, query);
    }

    private void queryAndSave(Table table, String query) {
        Writer writer = writers.computeIfAbsent(table, k -> storage.newWriter(table.getName()));
        Consumer<Map<String, Object>> processor = row -> {
            try {
                prepareRow(table, row);
                try {
                    storage.addToFile(writer, row);
                } catch (Throwable e) {
                    log.error("[{}] Failed to add row to file: {} (query {})", table.getName(), row, query, e);
                    throw e;
                }
                reportProcessed(table.getName(), row);
                for (Table relatedTable : relatedTables) {
                    if (skippedTables.contains(relatedTable)) {
                        continue;
                    }
                    if (!relatedTable.getReference().getValue().contains(table)) {
                        continue;
                    }
                    String relatedQuery;
                    if (relatedTable.getCustomSelect() == null) {
                        relatedQuery = format("SELECT %s.* FROM %s WHERE ", relatedTable.getName(), relatedTable.getName());
                    } else {
                        relatedQuery = relatedTable.getCustomSelect().apply(null);
                    }
                    relatedQuery = format(insertAfter(relatedQuery, "SELECT", " '%s' as table_name, "), table.toString());
                    relatedQuery += format("%s = '%s'", relatedTable.getReference().getKey(), row.get("id"));
                    relatedQuery += " ORDER BY " + String.join(", ", relatedTable.getSortColumns());
                    queryAndSave(relatedTable, relatedQuery);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        if (!table.isPartitioned()) {
            query(query, processor);
        } else {
            partitionService.getPartitions(table).forEach((partitionStart, partitionEnd) -> {
                String tsFilter = format(" %s.%s >= %s AND %s.%s < %s AND ", table.getName(), table.getPartitionColumn(),
                        partitionStart, table.getName(), table.getPartitionColumn(), partitionEnd);
                query(insertAfter(query, "WHERE", tsFilter), processor);
            });
        }
    }

    private void prepareRow(Table table, Map<String, Object> row) {
        if (table == Table.OTA_PACKAGE) {
            Long dataOid = (Long) row.get("data");
            if (dataOid != null) {
                row.put("data", postgresService.getBlob(dataOid));
            }
        }
    }

    private void query(String query, Consumer<Map<String, Object>> rowProcessor, Object... queryParams) {
        int batchIndex = 0;

        boolean hasNextBatch = true;
        while (hasNextBatch) {
            int offset = batchIndex * batchSize;
            String batchQuery = query + " LIMIT " + batchSize + " OFFSET " + offset;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(batchQuery, queryParams);
            rows.forEach(rowProcessor);
            batchIndex++;
            if (rows.size() < batchSize) {
                hasNextBatch = false;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(delayBetweenQueries);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String insertAfter(String input, String searchSequence, String value) {
        int afterSeq = StringUtils.indexOf(input, searchSequence) + searchSequence.length();
        return input.substring(0, afterSeq) + value + input.substring(afterSeq);
    }

}
