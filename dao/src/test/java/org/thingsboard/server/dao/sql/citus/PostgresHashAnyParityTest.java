// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresHashAnyParityTest extends AbstractCitusContainerTest {

    @Test
    void javaHashMatchesWorkerHashForManyUuids() {
        for (int i = 0; i < 5000; i++) {
            UUID uuid = UUID.randomUUID();
            int expected = jdbcTemplate.queryForObject("SELECT worker_hash(?::uuid)", Integer.class, uuid.toString());
            int actual = PostgresHashAny.hashUuid(uuid);
            assertThat(actual)
                    .as("worker_hash mismatch for %s", uuid)
                    .isEqualTo(expected);
        }
    }
}
