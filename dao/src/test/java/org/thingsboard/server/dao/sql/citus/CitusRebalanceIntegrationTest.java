// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the key Citus invariant: a shard rebalance moves shard-to-worker placement but does NOT
 * change the hash ranges. After {@code rebalance_table_shards}, all rows remain readable, the sorted
 * shard-range list is identical, and {@link CitusShardLocator#bucket(UUID)} is unchanged for every id.
 * <p>
 * Requires a second worker, which this test stands up itself on the inherited {@link #NETWORK} so the
 * shared single-worker harness is not slowed for sibling tests.
 */
class CitusRebalanceIntegrationTest extends AbstractCitusContainerTest {

    private static final String WORKER2_ALIAS = "worker2";
    private static final int SHARD_COUNT = 16;
    private static final int ROWS = 300;
    private static final int SAMPLE_SIZE = 20;

    private static final JdbcDatabaseContainer<?> WORKER2 = newWorker(WORKER2_ALIAS);

    @AfterAll
    static void stopWorker2() {
        deregisterAndStopWorker(WORKER2, WORKER2_ALIAS, "attribute_kv");
    }

    @Test
    void rebalancePreservesRowsAndBuckets() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv CASCADE");
        jdbcTemplate.execute("CREATE TABLE attribute_kv (entity_id uuid, attribute_type int, attribute_key int, " +
                "long_v bigint, version bigint, " +
                "CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key))");
        jdbcTemplate.execute("SELECT create_distributed_table('attribute_kv','entity_id', shard_count => " + SHARD_COUNT + ")");

        CitusShardLocator locator = new CitusShardLocator(jdbcTemplate, newCitusSettings(SHARD_COUNT), "attribute_kv");
        locator.refresh();

        // insert N rows with random UUID entity ids
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            UUID id = UUID.randomUUID();
            ids.add(id);
            jdbcTemplate.update(
                    "INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, long_v, version) " +
                            "VALUES (?, 1, 1, ?, 1)", id, (long) i);
        }

        // capture pre-rebalance truth
        long countBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM attribute_kv", Long.class);
        assertThat(countBefore).isEqualTo(ROWS);

        Map<UUID, Integer> sampleBuckets = new LinkedHashMap<>();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            UUID id = ids.get(i);
            sampleBuckets.put(id, locator.bucket(id));
        }

        List<String> rangesBefore = readSortedRanges();
        assertThat(rangesBefore).hasSize(SHARD_COUNT);

        // register worker2 (starts its container, installs the extension, adds the node) and rebalance. Registration
        // happens HERE, after the pre-rebalance truth is captured on the single worker, so the rebalance genuinely
        // moves placements. Use block_writes transfer mode: the default uses logical replication (requires
        // wal_level=logical) which is not configured in the container.
        registerWorker(WORKER2, WORKER2_ALIAS);
        jdbcTemplate.execute("SELECT rebalance_table_shards('attribute_kv', shard_transfer_mode => 'block_writes')");

        // verify the rebalance actually moved at least one shard placement onto worker2
        Integer worker2Group = jdbcTemplate.queryForObject(
                "SELECT groupid FROM pg_dist_node WHERE nodename = ?", Integer.class, WORKER2_ALIAS);
        assertThat(worker2Group).as("worker2 should be registered as a node").isNotNull();
        long shardsOnWorker2 = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_dist_placement p " +
                        "JOIN pg_dist_shard s ON s.shardid = p.shardid " +
                        "WHERE p.groupid = ? AND s.logicalrelid = 'attribute_kv'::regclass", Long.class, worker2Group);
        assertThat(shardsOnWorker2).as("rebalance should have moved at least one shard to worker2").isPositive();

        // (a) all rows still readable
        long countAfter = jdbcTemplate.queryForObject("SELECT count(*) FROM attribute_kv", Long.class);
        assertThat(countAfter).isEqualTo(ROWS);

        // (b) buckets unchanged after a fresh refresh
        locator.refresh();
        for (Map.Entry<UUID, Integer> e : sampleBuckets.entrySet()) {
            assertThat(locator.bucket(e.getKey()))
                    .as("bucket for %s must be stable across rebalance", e.getKey())
                    .isEqualTo(e.getValue());
        }

        // (c) sorted shard-range list byte-identical (only placement moved, not ranges)
        List<String> rangesAfter = readSortedRanges();
        assertThat(rangesAfter).isEqualTo(rangesBefore);

        // (d) spot-check a few rows read back with correct long_v
        for (int i = 0; i < 5; i++) {
            UUID id = ids.get(i);
            Long longV = jdbcTemplate.queryForObject(
                    "SELECT long_v FROM attribute_kv WHERE entity_id = ? AND attribute_type = 1 AND attribute_key = 1",
                    Long.class, id);
            assertThat(longV).isEqualTo((long) i);
        }
    }

    private List<String> readSortedRanges() {
        return jdbcTemplate.query(
                "SELECT shardminvalue, shardmaxvalue FROM pg_dist_shard " +
                        "WHERE logicalrelid = 'attribute_kv'::regclass ORDER BY shardminvalue::bigint",
                (rs, rowNum) -> rs.getString("shardminvalue") + ":" + rs.getString("shardmaxvalue"));
    }
}
