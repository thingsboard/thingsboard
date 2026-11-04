// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultCitusSchemaServiceSqlTest {

    private List<String> capturedDdl(int shardCount) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(String.class))).thenAnswer(invocation -> {
            String query = invocation.getArgument(0, String.class);
            // currentlyDistributed() -> empty: nothing distributed yet (fresh install)
            if (query.contains("pg_dist_partition")) {
                return List.of();
            }
            // existingForeignKeyStructures() -> every managed FK structurally present (a fresh schema has them
            // all), so the start-of-run crash repair finds nothing missing and re-adds nothing
            return CitusTables.MANAGED_FOREIGN_KEYS.stream()
                    .map(CitusTables.ManagedForeignKey::structuralKey)
                    .toList();
        });
        // columnExists() -> true: the alarm-group pre-flight checks originator_id column existence (via
        // information_schema.columns) before probing for NULL rows; the fixture schema has the column, so the
        // guard proceeds to the NULL probe. The remaining unstubbed Boolean catalog probes (NULL-originator rows,
        // legacy alarm_comment FK, widened-PK detection) default to null -> treated as false.
        when(jdbc.queryForObject(contains("information_schema.columns"), eq(Boolean.class), any(), any()))
                .thenReturn(Boolean.TRUE);
        CitusSettings settings = mock(CitusSettings.class);
        when(settings.getShardCount()).thenReturn(shardCount);

        new DefaultCitusSchemaService(jdbc, settings).applyDistribution();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        // atLeastOnce() collects every execute() invocation; the tests assert membership/ordering of the
        // statements they care about, deliberately NOT the exact statement count (an exact-count pin would
        // fail all tests at once with an opaque Mockito error whenever any single DDL statement is added
        // or removed; the real conversion behavior is covered by the container-backed DefaultCitusSchemaServiceTest).
        verify(jdbc, atLeastOnce()).execute(sql.capture());
        return sql.getAllValues();
    }

    @Test
    void anchorCarriesShardCountAndEntityId() {
        assertThat(capturedDdl(32))
                .contains("SELECT create_distributed_table('attribute_kv', 'entity_id', shard_count => 32)");
    }

    @Test
    void tsKvLatestColocatesOnEntityId() {
        assertThat(capturedDdl(32))
                .contains("SELECT create_distributed_table('ts_kv_latest', 'entity_id', colocate_with => 'attribute_kv')");
    }

    @Test
    void deviceColocatesOnId() {
        assertThat(capturedDdl(32))
                .contains("SELECT create_distributed_table('device', 'id', colocate_with => 'attribute_kv')");
    }

    @Test
    void assetColocatesOnId() {
        assertThat(capturedDdl(32))
                .contains("SELECT create_distributed_table('asset', 'id', colocate_with => 'attribute_kv')");
    }

    @Test
    void entityViewColocatesOnId() {
        assertThat(capturedDdl(32))
                .contains("SELECT create_distributed_table('entity_view', 'id', colocate_with => 'attribute_kv')");
    }

    @Test
    void deviceCredentialsCreatedAsReferenceTable() {
        assertThat(capturedDdl(32))
                .contains("SELECT create_reference_table('device_credentials')");
    }

    @Test
    void referenceTablesCreatedBeforeDistributedTables() {
        // Intra-reference-list order is deliberately NOT asserted: it is membership-only and not
        // load-bearing (all managed FKs are dropped before conversion — see CitusTables.REFERENCE_TABLES).
        // Only the phase ordering matters: every reference table before any distributed table.
        List<String> ddl = capturedDdl(32);
        int keyDict = ddl.indexOf("SELECT create_reference_table('key_dictionary')");
        int relation = ddl.indexOf("SELECT create_reference_table('relation')");
        int device = ddl.indexOf("SELECT create_distributed_table('device', 'id', colocate_with => 'attribute_kv')");
        assertThat(keyDict).isGreaterThanOrEqualTo(0);
        assertThat(relation).isGreaterThanOrEqualTo(0);
        assertThat(device).isGreaterThan(keyDict);
        assertThat(device).isGreaterThan(relation);
    }
}
