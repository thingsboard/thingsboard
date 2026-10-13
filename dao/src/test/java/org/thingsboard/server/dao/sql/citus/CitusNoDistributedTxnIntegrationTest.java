// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.dao.service.CitusTestSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that hot-path KV writes route to a single Citus shard when batches are shard-aligned through
 * {@link CitusShardLocator}, so they execute as single-shard transactions and never incur the distributed
 * (2-phase-commit) overhead. Also proves that a deliberately cross-shard write still inserts correct data
 * (it merely degrades to a multi-shard modification; it never corrupts).
 *
 * <p>Oracle: Citus {@code EXPLAIN} "Task Count". A single-shard router-executable write shows
 * {@code Task Count: 1}; a multi-shard write shows {@code Task Count: > 1}. This is preferred over counting
 * {@code pg_dist_transaction} rows, whose 2PC bookkeeping is transient (cleaned up on commit) and therefore
 * unreliable to observe after the write completes.
 */
class CitusNoDistributedTxnIntegrationTest extends AbstractCitusContainerTest {

    private static final int SHARD_COUNT = 16;

    private CitusShardLocator locator;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv CASCADE");
        jdbcTemplate.execute("CREATE TABLE attribute_kv (entity_id uuid, attribute_type int, attribute_key int, long_v bigint, version bigint, " +
                "CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key))");
        jdbcTemplate.execute("SELECT create_distributed_table('attribute_kv','entity_id', shard_count => " + SHARD_COUNT + ")");
        locator = new CitusShardLocator(jdbcTemplate, newCitusSettings(SHARD_COUNT), "attribute_kv");
        locator.refresh();
    }

    @Test
    void singleBucketMultiRowInsertRoutesToOneShard() {
        Map<Integer, List<UUID>> byBucket = generateIdsByBucket(400);
        List<UUID> sameBucket = byBucket.values().stream()
                .filter(ids -> ids.size() >= 3)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no bucket with >= 3 ids"));

        String insert = buildInsert(sameBucket.subList(0, 3));
        assertThat(CitusTestSupport.taskCount(jdbcTemplate, insert))
                .as("a shard-aligned multi-row insert must route to a single shard (no distributed transaction)")
                .isEqualTo(1);
    }

    @Test
    void crossBucketInsertSpansShardsButKeepsDataCorrect() {
        Map<Integer, List<UUID>> byBucket = generateIdsByBucket(400);
        List<List<UUID>> distinctBuckets = byBucket.values().stream()
                .filter(ids -> !ids.isEmpty())
                .limit(2)
                .toList();
        assertThat(distinctBuckets).as("need ids in two distinct buckets").hasSize(2);

        UUID a = distinctBuckets.get(0).get(0);
        UUID b = distinctBuckets.get(1).get(0);
        assertThat(locator.bucket(a)).as("ids must be in different buckets").isNotEqualTo(locator.bucket(b));

        String insert = buildInsert(List.of(a, b));
        assertThat(CitusTestSupport.taskCount(jdbcTemplate, insert))
                .as("a cross-shard insert degrades to a multi-shard modification")
                .isGreaterThan(1);

        // Run the insert for real and confirm both rows landed correctly - degraded routing must not corrupt data.
        jdbcTemplate.update(insert);
        Long rows = jdbcTemplate.queryForObject(
                "select count(*) from attribute_kv where entity_id in (?::uuid, ?::uuid) and long_v = 1 and version = 1",
                Long.class, a.toString(), b.toString());
        assertThat(rows).as("both rows must be present with the expected values").isEqualTo(2L);
    }

    private Map<Integer, List<UUID>> generateIdsByBucket(int count) {
        Map<Integer, List<UUID>> byBucket = new HashMap<>();
        for (int i = 0; i < count; i++) {
            UUID id = UUID.randomUUID();
            byBucket.computeIfAbsent(locator.bucket(id), k -> new ArrayList<>()).add(id);
        }
        return byBucket;
    }

    /**
     * Builds a multi-row INSERT with the entity ids inlined as literals (one distinct attribute_key per row so
     * same-bucket ids do not collide on the primary key). Literal inlining is required because Citus does not
     * support {@code EXPLAIN} of a parameterized DML; the values are server-generated UUIDs, not user input.
     */
    private String buildInsert(List<UUID> ids) {
        StringBuilder sql = new StringBuilder("INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, long_v, version) VALUES ");
        for (int i = 0; i < ids.size(); i++) {
            sql.append(i == 0 ? "" : ",").append("('").append(ids.get(i)).append("'::uuid,1,").append(i).append(",1,1)");
        }
        return sql.toString();
    }
}
