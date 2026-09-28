// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic known-answer test for {@link PostgresHashAny}.
 * <p>
 * Unlike {@link PostgresHashAnyParityTest}, this test does NOT require Docker / a Citus container: it
 * hardcodes a handful of fixed UUID -> {@code worker_hash} integer vectors and asserts that the pure-Java
 * {@link PostgresHashAny#hashUuid(UUID)} reproduces them. This catches hash regressions on every build,
 * even on machines without Docker.
 * <p>
 * The expected integers are NOT derived from this implementation (that would be a vacuous self-consistency
 * check). They were captured from the real database — {@code SELECT worker_hash(?::uuid)} running on the
 * pinned Citus image ({@link CitusClusterSupport#CITUS_IMAGE}, currently {@code citusdata/citus:12.1}) —
 * which is the source of truth for Citus shard routing.
 */
class PostgresHashAnyKnownAnswerTest {

    @Test
    void hashUuidMatchesCapturedWorkerHashValues() {
        // UUID -> worker_hash(uuid), captured from citusdata/citus:12.1 via SELECT worker_hash(?::uuid).
        assertWorkerHash("00000000-0000-0000-0000-000000000000", 1353656403);
        assertWorkerHash("ffffffff-ffff-ffff-ffff-ffffffffffff", -682383255);
        assertWorkerHash("40b61b39-0b5c-499a-8e26-44ee28669741", 860474987);
        assertWorkerHash("12345678-1234-5678-1234-567812345678", 810843902);
        assertWorkerHash("deadbeef-dead-beef-dead-beefdeadbeef", 1052026665);
        assertWorkerHash("a1b2c3d4-e5f6-7890-abcd-ef0123456789", 1340407995);
    }

    private static void assertWorkerHash(String uuid, int expectedWorkerHash) {
        assertThat(PostgresHashAny.hashUuid(UUID.fromString(uuid)))
                .as("worker_hash mismatch for %s", uuid)
                .isEqualTo(expectedWorkerHash);
    }
}
