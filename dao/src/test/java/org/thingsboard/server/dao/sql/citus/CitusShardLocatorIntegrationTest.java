// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CitusShardLocatorIntegrationTest extends AbstractCitusContainerTest {

    private CitusShardLocator locator;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS ts_kv_latest CASCADE");
        jdbcTemplate.execute("CREATE TABLE ts_kv_latest (entity_id uuid, key int, version bigint, " +
                "CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key))");
        jdbcTemplate.execute("SELECT create_distributed_table('ts_kv_latest','entity_id', shard_count => 16)");

        locator = new CitusShardLocator(jdbcTemplate, newCitusSettings(16), "ts_kv_latest");
        locator.refresh();
    }

    @Test
    void exposesShardCount() {
        assertThat(locator.shardCount()).isEqualTo(16);
    }

    @Test
    void bucketMatchesCitusRoutingForManyUuids() {
        // Build the shard-id -> bucket map exactly as the locator does: shard ids sorted by min range.
        List<Long> orderedShardIds = jdbcTemplate.queryForList(
                "select shardid from pg_dist_shard where logicalrelid='ts_kv_latest'::regclass " +
                        "order by (shardminvalue)::int", Long.class);

        Map<Integer, Long> bucketToShard = new HashMap<>();
        for (int i = 0; i < 2000; i++) {
            UUID uuid = UUID.randomUUID();
            long citusShardId = jdbcTemplate.queryForObject(
                    "select get_shard_id_for_distribution_column('ts_kv_latest', ?::uuid)", Long.class, uuid.toString());
            int expectedBucket = orderedShardIds.indexOf(citusShardId);
            assertThat(locator.bucket(uuid))
                    .as("bucket mismatch for %s", uuid)
                    .isEqualTo(expectedBucket);
            bucketToShard.put(expectedBucket, citusShardId);
        }
        // and the bucket -> shard mapping is injective: no two buckets may share a physical shard
        assertThat(bucketToShard.values()).doesNotHaveDuplicates();
    }
}
