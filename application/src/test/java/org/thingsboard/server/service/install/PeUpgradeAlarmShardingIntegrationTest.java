// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * CE-to-PE twin of the 4.3.1.4 alarm-sharding coverage in {@code LtsMigrationIntegrationTest}: the same schema
 * change (alarm_comment FK drop, entity_alarm.originator_id ADD COLUMN) plus the deliberately single-statement
 * originator backfill live in upgrade/pe/schema_update.sql and are executed by no other test. The whole file is
 * idempotent by design (SqlDatabaseUpgradeService sends it as one execute), so the test runs it verbatim — twice,
 * to also pin that a re-run is a no-op.
 */
@DaoSqlTest
public class PeUpgradeAlarmShardingIntegrationTest extends AbstractControllerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InstallScripts installScripts;

    @Test
    public void peSchemaUpdateDropsAlarmCommentFkAndBackfillsEntityAlarmOriginatorIdempotently() {
        // The test-context schema already ships the PE end state (entity_alarm.originator_id present,
        // alarm_comment -> alarm FK absent), so reconstruct the pre-upgrade CE state first.
        // Comment cleanup on alarm deletion is asynchronous now (Housekeeper), so an earlier test class in the
        // shared suite DB may have been torn down with alarm_comment rows whose alarm is already gone; the plain
        // ADD CONSTRAINT below validates every existing row, so such orphans must be removed first.
        jdbcTemplate.execute("DELETE FROM alarm_comment WHERE alarm_id NOT IN (SELECT id FROM alarm)");
        jdbcTemplate.execute("ALTER TABLE alarm_comment ADD CONSTRAINT fk_alarm_comment_alarm_id " +
                "FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE");
        jdbcTemplate.execute("ALTER TABLE entity_alarm DROP COLUMN IF EXISTS originator_id");

        assertTrue(constraintExists("fk_alarm_comment_alarm_id"));
        assertFalse(columnExists("entity_alarm", "originator_id"));

        // Seed an alarm owned by originatorA, propagated to a DIFFERENT entity B (so the entity_alarm row's
        // entity_id is B, not the originator). The pre-upgrade entity_alarm row has no originator_id column yet.
        UUID alarmId = UUID.randomUUID();
        UUID originatorA = UUID.randomUUID();
        UUID entityB = UUID.randomUUID();
        UUID tenantUuid = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO alarm (id, created_time, originator_id, originator_type, tenant_id, type) " +
                "VALUES (?, ?, ?, ?, ?, ?)", alarmId, 1L, originatorA, 1, tenantUuid, "General");
        jdbcTemplate.update("INSERT INTO entity_alarm (tenant_id, entity_type, entity_id, created_time, alarm_type, alarm_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)", tenantUuid, "ASSET", entityB, 1L, "General", alarmId);

        try {
            runPeSchemaUpdate();

            // The alarm-sharding block ran: the cascade FK is gone, the column is back, and the single-statement
            // backfill filled the propagated row with the parent alarm's originator (A), not the row's entity (B).
            assertFalse(constraintExists("fk_alarm_comment_alarm_id"));
            assertTrue(columnExists("entity_alarm", "originator_id"));
            assertEquals(originatorA, findEntityAlarmOriginator(entityB, alarmId));

            // The whole file is idempotent: a re-run changes nothing and throws nothing (the backfill's IS NULL
            // guard gives its second pass zero rows to update).
            runPeSchemaUpdate();
            assertFalse(constraintExists("fk_alarm_comment_alarm_id"));
            assertEquals(originatorA, findEntityAlarmOriginator(entityB, alarmId));
        } finally {
            // If the upgrade SQL threw before its ADD COLUMN ran, the column this test dropped would stay missing
            // and break every subsequent alarm test in the shared suite; restore it unconditionally and re-fill the
            // originators the DROP COLUMN destroyed on rows owned by other test classes.
            jdbcTemplate.execute("ALTER TABLE entity_alarm ADD COLUMN IF NOT EXISTS originator_id uuid");
            jdbcTemplate.execute("UPDATE entity_alarm ea SET originator_id = a.originator_id FROM alarm a " +
                    "WHERE a.id = ea.alarm_id AND ea.originator_id IS NULL");
            // Leave the shared suite schema clean: drop the seeded rows and the FK, restoring the base end state.
            jdbcTemplate.update("DELETE FROM entity_alarm WHERE alarm_id = ?", alarmId);
            jdbcTemplate.update("DELETE FROM alarm WHERE id = ?", alarmId);
            jdbcTemplate.execute("ALTER TABLE alarm_comment DROP CONSTRAINT IF EXISTS fk_alarm_comment_alarm_id");
        }
    }

    // Executes upgrade/pe/schema_update.sql the way SqlDatabaseUpgradeService.upgradeDatabase(true) does: the whole
    // file as a single execute.
    private void runPeSchemaUpdate() {
        try {
            String sql = Files.readString(Paths.get(installScripts.getDataDir(), "upgrade", "pe", "schema_update.sql"));
            jdbcTemplate.execute(sql);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to run the CE-to-PE schema_update.sql", e);
        }
    }

    private UUID findEntityAlarmOriginator(UUID entityId, UUID alarmId) {
        return jdbcTemplate.queryForObject(
                "SELECT originator_id FROM entity_alarm WHERE entity_id = ? AND alarm_id = ?",
                UUID.class, entityId, alarmId);
    }

    private boolean columnExists(String table, String column) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = ? AND column_name = ?)",
                Boolean.class, table, column);
        return Boolean.TRUE.equals(exists);
    }

    private boolean constraintExists(String constraintName) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ?)", Boolean.class, constraintName);
        return Boolean.TRUE.equals(exists);
    }
}
