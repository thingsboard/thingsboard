/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.install.lts;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Drives {@link V4_3_1_4Migration#applyAfterCommit()} against a real Postgres instance with a batch size far
 * smaller than the legacy backlog seeded here, so the multi-batch keyset-pagination loop (cursor advance, guard,
 * termination) actually exercises more than one window -- not just the single-batch happy path.
 */
@DaoSqlTest
@TestPropertySource(properties = "install.rpc_legacy_cleanup_batch_size=2")
public class V4_3_1_4MigrationIntegrationTest extends AbstractControllerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private V4_3_1_4Migration migration;

    private final List<UUID> seededIds = new ArrayList<>();

    @After
    public void tearDown() {
        if (!seededIds.isEmpty()) {
            jdbcTemplate.batchUpdate("DELETE FROM rpc WHERE id = ?",
                    seededIds.stream().map(id -> new Object[]{id}).toList());
        }
    }

    @Test
    public void batchLoopClosesAllStuckRowsAcrossMultipleWindowsAndLeavesOthersUntouched() {
        // batchSize is wired to 2 via the class-level @TestPropertySource (< the 6 stuck rows seeded below), so
        // applyAfterCommit() must run several keyset-paginated windows, proving the cursor actually advances
        // across batches instead of just once.

        DeviceId deviceId = new DeviceId(UUID.fromString("39813cdc-a1ae-47c7-92af-25ec0a1fc303"));
        long now = System.currentTimeMillis();
        long past = now - 60_000;
        long future = now + 60_000;

        // Legacy (request_id IS NULL) rows the backfill MUST close -- 6 rows against a batch size of 2, so the
        // loop must span 3 windows (2 + 2 + 2) for the cursor logic to be genuinely exercised.
        List<UUID> stuck = List.of(
                saveRpc(deviceId, UUID.fromString("9244c6c6-d932-43ca-91bb-b24482b08905"), "DELIVERED", false, past, null),
                saveRpc(deviceId, UUID.fromString("c67c7357-1198-40f5-9d07-1a4a9f862720"), "DELIVERED", false, past, null),
                saveRpc(deviceId, UUID.fromString("bde89223-7fe0-47e3-b190-88925ac7d070"), "DELIVERED", false, past, null),
                saveRpc(deviceId, UUID.fromString("564b8b9d-5e06-4a71-b9d9-81b5b736595c"), "DELIVERED", false, past, null),
                saveRpc(deviceId, UUID.fromString("31587a70-50c3-4f20-872c-70d4d3406a2d"), "SENT", true, past, null),
                saveRpc(deviceId, UUID.fromString("1f6d2a4e-0c1b-4b7a-8e2d-2b9c7a1e6f30"), "TIMEOUT", false, past, null));

        // Rows the backfill MUST leave untouched.
        UUID oneWayDeliveredPastExpiry = saveRpc(deviceId, UUID.fromString("a553c35b-47a4-4906-825a-1be743e96d5d"), "DELIVERED", true, past, null); // terminal success
        UUID twoWayDeliveredFutureExpiry = saveRpc(deviceId, UUID.fromString("acfd787f-86f3-4121-becb-e88eaa288934"), "DELIVERED", false, future, null); // not expired yet
        UUID twoWayDeliveredWithRequestId = saveRpc(deviceId, UUID.fromString("ae7af8fc-7e58-492a-8c56-7453a48b3263"), "DELIVERED", false, past, 7); // tracked, not legacy
        UUID queuedPastExpiry = saveRpc(deviceId, UUID.fromString("06129c0a-6654-49b0-938a-b87b79a8b7b7"), "QUEUED", false, past, null); // never sent

        migration.applyAfterCommit();

        for (UUID id : stuck) {
            assertEquals("EXPIRED", statusOf(id));
            assertNotNull(responseOf(id));
        }

        assertEquals("DELIVERED", statusOf(oneWayDeliveredPastExpiry));
        assertEquals("DELIVERED", statusOf(twoWayDeliveredFutureExpiry));
        assertEquals("DELIVERED", statusOf(twoWayDeliveredWithRequestId));
        assertEquals("QUEUED", statusOf(queuedPastExpiry));
    }

    private String statusOf(UUID id) {
        return jdbcTemplate.queryForObject("SELECT status FROM rpc WHERE id = ?", String.class, id);
    }

    private String responseOf(UUID id) {
        return jdbcTemplate.queryForObject("SELECT response FROM rpc WHERE id = ?", String.class, id);
    }

    // Seeds an rpc row (bypassing the entity/JPA layer to write raw request JSON) exactly as a pre-request_id
    // server version would have left it: request_id NULL, request JSON carrying the oneway flag the migration's
    // CLEANUP_BATCH_SQL extracts via request::jsonb ->> 'oneway'.
    private UUID saveRpc(DeviceId deviceId, UUID id, String status, boolean oneway, long expirationTime, Integer requestId) {
        seededIds.add(id);
        String request = "{\"oneway\":" + oneway + ",\"method\":\"x\"}";
        jdbcTemplate.update("INSERT INTO rpc (id, created_time, tenant_id, device_id, expiration_time, request, " +
                        "response, status, request_id, oneway) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)",
                id, System.currentTimeMillis(), TenantId.SYS_TENANT_ID.getId(), deviceId.getId(), expirationTime,
                request, status, requestId, oneway);
        return id;
    }
}
