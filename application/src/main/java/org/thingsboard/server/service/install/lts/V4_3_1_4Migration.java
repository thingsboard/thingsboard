// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Alarm sharding: drops the {@code alarm_comment -> alarm} cascade FK and adds {@code entity_alarm.originator_id}.
 * <p>
 * {@link LtsMigrationService} selects migrations from the injected {@link LtsMigration} beans, not from the on-disk
 * {@code data/upgrade/lts/<version>/} directories, so this bean is what makes the runner discover version
 * {@code 4.3.1.4} and execute its {@code schema_update.sql} at all.
 * <p>
 * The one-shot {@link #applyAfterCommit()} backfill cannot see every affected row during a mixed-version rolling
 * upgrade: an old node that has not yet been restarted keeps inserting {@code entity_alarm} rows with a {@code NULL}
 * {@code originator_id} behind and after the keyset walk, and once the version is recorded the version-gated patch
 * path never runs the backfill again. Rather than carry any permanent repair machinery, this migration merely logs
 * an advisory telling the sysadmin to verify once the whole cluster is upgraded.
 * <p>
 * The Citus distribution of {@code alarm}/{@code entity_alarm} on {@code originator_id} is NOT part of this
 * migration: it is applied only on a fresh Citus install or the explicit {@code postgres-to-citus} conversion (see
 * {@code DefaultCitusSchemaService.applyDistribution}).
 */
@Slf4j
@Component
@TbCoreComponent
public class V4_3_1_4Migration implements LtsMigration {

    // entity_alarm is walked in keyset-paginated windows of this size, each in its own committed transaction, so
    // the backfill only ever holds row-level locks on the current window and never blocks concurrent readers.
    // Smaller windows mean more round-trips over the table; larger ones keep each window's transaction (and its
    // row locks) open longer. Package-private so tests can shrink it to exercise multi-window pagination.
    @Value("${install.entity_alarm_backfill_batch_size:50000}")
    int batchSize;

    // Smallest possible (entity_id, alarm_id) key -- the initial keyset cursor. entity_id is always a real
    // entity's id, never the nil UUID, so starting strictly greater than this misses no row.
    private static final UUID MIN_UUID = new UUID(0L, 0L);

    // Walks entity_alarm once in primary-key order (entity_id, alarm_id), filling originator_id from the parent
    // alarm for the rows still NULL in the current window. There is deliberately NO index on
    // entity_alarm.originator_id, so a "WHERE originator_id IS NULL LIMIT n" loop would re-seq-scan the table
    // every batch -- quadratic, because each batch must first skip all the rows earlier batches already
    // backfilled. Keyset-paginating on the primary key instead makes each window a covering index range scan and
    // the whole backfill a single linear pass. fk_entity_alarm_id guarantees every entity_alarm has a matching
    // alarm, so every NULL row is fillable. The window CTE feeds both the data-modifying CTE and the final
    // SELECT, which returns how many rows this batch updated and the window's last key (the next cursor); an
    // empty result means the walk is done.
    private static final String BACKFILL_BATCH_SQL =
            "WITH w AS (" +
            "  SELECT entity_id, alarm_id FROM entity_alarm " +
            "  WHERE (entity_id, alarm_id) > (?, ?) " +
            "  ORDER BY entity_id, alarm_id LIMIT ?" +
            "), u AS (" +
            "  UPDATE entity_alarm ea SET originator_id = a.originator_id " +
            "  FROM alarm a, w " +
            "  WHERE ea.entity_id = w.entity_id AND ea.alarm_id = w.alarm_id " +
            "    AND a.id = ea.alarm_id AND ea.originator_id IS NULL " +
            "  RETURNING 1" +
            ") " +
            "SELECT (SELECT count(*) FROM u) AS updated_count, w.entity_id, w.alarm_id " +
            "FROM w ORDER BY w.entity_id DESC, w.alarm_id DESC LIMIT 1";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public V4_3_1_4Migration(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public String getVersion() {
        return "4.3.1.4";
    }

    /**
     * Backfills entity_alarm.originator_id from the parent alarm for rows that predate the column added by this
     * version's schema_update.sql. Runs OUTSIDE the schema transaction (see {@link LtsMigration#applyAfterCommit})
     * so the ADD COLUMN's ACCESS EXCLUSIVE lock is already released; each batch commits on its own and takes only
     * a ROW EXCLUSIVE lock, so a serving node keeps reading and writing entity_alarm throughout.
     * <p>
     * New entity_alarm rows always set originator_id at insert time (see {@code BaseAlarmService}), so only rows
     * that existed before the upgrade are ever NULL. On the upgrade run every pre-existing row is NULL and gets
     * filled in a single linear pass over the primary-key index. A redundant re-run (e.g. after a crash, or on an
     * already-populated table) still walks the table once via that index but updates nothing -- the {@code IS NULL}
     * filter makes it a no-op, which is what keeps the method restart-safe.
     */
    @Override
    public void applyAfterCommit() {
        // On the no-downtime patch path this runs on a SERVING node, and every entity_alarm row that predates the
        // upgrade is invisible to entity-scoped alarm queries (which join on a.originator_id = ea.originator_id)
        // until the backfill fills its window. Announce the read gap up front so an operator watching the log knows
        // it is expected; the gap closes window by window and disappears once the backfill finishes.
        log.warn("""
                Starting the entity_alarm.originator_id backfill: until it completes, alarms whose entity_alarm rows
                are not yet backfilled stay invisible to entity-scoped alarm queries. The backfill walks the table in
                batches without blocking reads or writes, so the gap shrinks as it proceeds.""");
        long total = backfillNullOriginators();
        if (total > 0) {
            log.info("Backfilled entity_alarm.originator_id for {} row(s)", total);
        }
        // The one-shot backfill is deliberately best-effort and cannot be complete: during a mixed-version rolling
        // upgrade a node still running a version older than 4.3.1.4 keeps inserting entity_alarm rows with a NULL
        // originator_id, and such stragglers can appear only after this already-upgraded node has finished its
        // backfill and recorded the version. That is why the advisory below is logged unconditionally (and no
        // straggler counting is attempted here -- a count would seq-scan the whole table on a serving node only to
        // produce a number the sysadmin must re-derive after the last node is upgraded anyway).
        log.warn("""
                Rolling-upgrade advisory: nodes still running a version older than 4.3.1.4 keep inserting
                entity_alarm rows with a NULL originator_id until the whole cluster is upgraded, and such rows are
                invisible to entity-scoped alarm queries. After ALL nodes are upgraded, verify and, if needed, repair manually.
                check:  SELECT count(*) FROM entity_alarm WHERE originator_id IS NULL;
                repair: UPDATE entity_alarm ea SET originator_id = a.originator_id FROM alarm a WHERE a.id = ea.alarm_id AND ea.originator_id IS NULL;""");
    }

    // Walks entity_alarm once in primary-key order and fills originator_id for every row still NULL, in
    // keyset-paginated self-committing batches; returns how many rows it filled.
    private long backfillNullOriginators() {
        long total = 0;
        UUID cursorEntityId = MIN_UUID;
        UUID cursorAlarmId = MIN_UUID;
        while (true) {
            UUID entityCursor = cursorEntityId;
            UUID alarmCursor = cursorAlarmId;
            BackfillBatch batch = transactionTemplate.execute(status ->
                    jdbcTemplate.query(BACKFILL_BATCH_SQL, this::extractBatch, entityCursor, alarmCursor, batchSize));
            if (batch == null) {
                break; // window empty -> the whole table has been walked
            }
            // Infinite-loop insurance: the window WHERE is "(entity_id, alarm_id) > (cursor)", so a non-empty
            // window's max key must strictly exceed the cursor it was queried with. Fail fast if a future change
            // to the SQL ever breaks that invariant instead of re-reading the same window forever.
            if (compareKeys(batch.lastEntityId(), batch.lastAlarmId(), entityCursor, alarmCursor) <= 0) {
                throw new IllegalStateException("entity_alarm backfill cursor did not advance: window last key ("
                        + batch.lastEntityId() + ", " + batch.lastAlarmId() + ") is not past cursor ("
                        + entityCursor + ", " + alarmCursor + ")");
            }
            total += batch.updatedCount();
            cursorEntityId = batch.lastEntityId();
            cursorAlarmId = batch.lastAlarmId();
        }
        return total;
    }

    private BackfillBatch extractBatch(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return new BackfillBatch(rs.getLong("updated_count"),
                rs.getObject("entity_id", UUID.class), rs.getObject("alarm_id", UUID.class));
    }

    // Compares composite (entity_id, alarm_id) keys the way PostgreSQL orders uuid columns: byte-wise, i.e. as
    // unsigned 128-bit values. Java's UUID.compareTo compares signed longs and would disagree once the high bit
    // is set, so it must not be used here.
    private static int compareKeys(UUID entityId1, UUID alarmId1, UUID entityId2, UUID alarmId2) {
        int result = compareUuids(entityId1, entityId2);
        return result != 0 ? result : compareUuids(alarmId1, alarmId2);
    }

    private static int compareUuids(UUID first, UUID second) {
        int result = Long.compareUnsigned(first.getMostSignificantBits(), second.getMostSignificantBits());
        return result != 0 ? result : Long.compareUnsigned(first.getLeastSignificantBits(), second.getLeastSignificantBits());
    }

    private record BackfillBatch(long updatedCount, UUID lastEntityId, UUID lastAlarmId) {}
}
