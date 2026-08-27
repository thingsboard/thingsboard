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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.device.DeviceActorMessageProcessor;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

/**
 * Registration-only migration with no {@link #apply()} (no data migration), plus an {@link #applyAfterCommit()}
 * backfill that closes the pre-existing legacy stuck-RPC backlog.
 * <p>
 * {@link LtsMigrationService} selects migrations from the injected {@link LtsMigration} beans, not from the
 * on-disk {@code data/upgrade/lts/<version>/} directories. So this bean is what makes the runner discover
 * version {@code 4.3.1.4} and execute its {@code data/upgrade/lts/4.3.1.4/schema_update.sql} (which adds the
 * nullable {@code rpc.request_id} and {@code rpc.oneway} columns). A directory holding a {@code schema_update.sql}
 * but lacking a matching bean would be silently skipped.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code LtsMigrationIntegrationTest}.
 * <p>
 * {@link #applyAfterCommit()} closes rows left stuck by earlier server versions that predate {@code request_id}
 * tracking: any {@code rpc} row with {@code request_id IS NULL} that is past its expiration and still sitting in
 * {@code SENT} or {@code TIMEOUT}, or in {@code DELIVERED} for a two-way call (one-way {@code DELIVERED} is already terminal
 * success), is force-closed to {@code EXPIRED} with a canned response. It walks the {@code rpc} table by primary
 * key in keyset-paginated, self-committing batches so it never holds a long transaction or blocks concurrent
 * readers/writers; see the class-level backfill contract on {@link LtsMigration#applyAfterCommit()}.
 */
@Slf4j
@Component
@TbCoreComponent
public class V4_3_1_4Migration implements LtsMigration {

    // Same wording as DeviceActorMessageProcessor.LEGACY_UNTRACKED_MESSAGE, so a row closed by this one-time
    // backfill reads identically to one closed live by the actor on reload.
    static final String LEGACY_CLOSE_RESPONSE = JacksonUtil.newObjectNode()
            .put("error", DeviceActorMessageProcessor.LEGACY_UNTRACKED_MESSAGE).toString();

    // Keyset-paginated batch: window of ids > cursor, close the matching legacy rows in that window, report how
    // many closed and the window's max id (the next cursor). Self-contained CTE so a single round trip both
    // selects the window and performs the conditional update.
    // The window scan (`id > ?`) walks the table in id order regardless of how many rows match — that's fine
    // here: during this one-time upgrade every legacy row has request_id IS NULL, so an index on that predicate
    // would not be selective and wouldn't speed anything up.
    static final String CLEANUP_BATCH_SQL = """
            WITH w AS (
              SELECT id FROM rpc WHERE id > ? ORDER BY id LIMIT ?
            ), u AS (
              UPDATE rpc SET status = 'EXPIRED', response = ?
              FROM w WHERE rpc.id = w.id
                AND rpc.request_id IS NULL
                AND rpc.expiration_time < ?
                AND (rpc.status IN ('SENT','TIMEOUT') OR (rpc.status = 'DELIVERED' AND (rpc.request::jsonb ->> 'oneway') = 'false'))
              RETURNING 1
            )
            SELECT (SELECT count(*) FROM u) AS updated_count,
                   (SELECT id FROM w ORDER BY id DESC LIMIT 1) AS last_id
            """;

    private static final UUID MIN_UUID = new UUID(0L, 0L);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${install.rpc_legacy_cleanup_batch_size:50000}")
    private int batchSize;

    public V4_3_1_4Migration(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public String getVersion() {
        return "4.3.1.4";
    }

    @Override
    public void applyAfterCommit() {
        long now = System.currentTimeMillis();
        UUID cursor = MIN_UUID;
        long totalClosed = 0;
        long totalBatches = 0;
        while (true) {
            BatchResult result = runBatch(cursor, now);
            if (result.lastId() == null) {
                break;
            }
            if (comparePgUuid(result.lastId(), cursor) <= 0) {
                // Defensive: the window is ORDER BY id ASC over id > cursor, so its max must strictly exceed
                // cursor. A non-increasing cursor would spin forever, so fail loudly instead.
                throw new IllegalStateException("Legacy RPC cleanup cursor failed to advance: cursor=" + cursor
                        + " lastId=" + result.lastId());
            }
            cursor = result.lastId();
            totalClosed += result.updatedCount();
            totalBatches++;
        }
        log.info("Legacy RPC backlog cleanup closed {} row(s) in {} batch(es)", totalClosed, totalBatches);
    }

    private BatchResult runBatch(UUID cursor, long now) {
        return transactionTemplate.execute(status -> jdbcTemplate.queryForObject(CLEANUP_BATCH_SQL,
                (rs, rowNum) -> new BatchResult(rs.getLong("updated_count"), (UUID) rs.getObject("last_id")),
                cursor, batchSize, LEGACY_CLOSE_RESPONSE, now));
    }

    // PostgreSQL orders uuid values byte-for-byte, i.e. as unsigned 128-bit integers -- NOT as Java's
    // UUID.compareTo, which treats the MSB/LSB longs as signed. Compare the same way the ORDER BY does.
    private static int comparePgUuid(UUID a, UUID b) {
        int cmp = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        if (cmp != 0) {
            return cmp;
        }
        return Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }

    private record BatchResult(long updatedCount, UUID lastId) {
    }
}
