// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CitusVersionIncrementIntegrationTest extends AbstractCitusContainerTest {

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS ts_kv_latest CASCADE");
        jdbcTemplate.execute("CREATE TABLE ts_kv_latest (entity_id uuid, key int, long_v bigint, version bigint, " +
                "CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key))");
        jdbcTemplate.execute("SELECT create_distributed_table('ts_kv_latest', 'entity_id', shard_count => 8)");
    }

    private long upsert(UUID entityId, int key, long value) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO ts_kv_latest (entity_id, key, long_v, version) VALUES (?, ?, ?, 1) " +
                        "ON CONFLICT (entity_id, key) DO UPDATE SET long_v = ?, version = ts_kv_latest.version + 1 " +
                        "RETURNING version",
                Long.class, entityId, key, value, value);
    }

    @Test
    void versionIncrementsPerEntityKey() {
        UUID e = UUID.randomUUID();
        assertThat(upsert(e, 1, 10)).isEqualTo(1);
        assertThat(upsert(e, 1, 11)).isEqualTo(2);
        assertThat(upsert(e, 1, 12)).isEqualTo(3);
    }

    @Test
    void differentKeysTrackIndependently() {
        UUID e = UUID.randomUUID();
        assertThat(upsert(e, 1, 10)).isEqualTo(1);
        assertThat(upsert(e, 2, 20)).isEqualTo(1); // independent counter, sharing value 1 is fine
        assertThat(upsert(e, 1, 11)).isEqualTo(2);
    }
}
