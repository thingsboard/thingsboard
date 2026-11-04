// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.system;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Pins the central claim of {@code SystemPatchApplier}'s schema-functions.sql replay: replaying it over a
 * database that still holds the previous release's function set is safe. The five alarm-mutation functions changed
 * arity in 4.3.1.4, and the replay must leave BOTH arities in pg_proc — the new implementation for upgraded nodes
 * and the old-arity compatibility wrapper for nodes still running the previous release during a rolling upgrade.
 */
@DaoSqlTest
public class SchemaFunctionsReplayIntegrationTest extends AbstractControllerTest {

    // For each arity-changed alarm function: the pre-4.3.1.4 argument type list (as the previous release shipped it)
    // and the current one, in oidvectortypes() rendering. Set.of order is irrelevant — assertions compare sets.
    private static final Map<String, Set<String>> EXPECTED_ARITIES = Map.of(
            "update_alarm", Set.of(
                    "uuid, uuid, character varying, bigint, bigint, character varying, boolean, boolean, boolean, boolean, character varying",
                    "uuid, uuid, uuid, character varying, bigint, bigint, character varying, boolean, boolean, boolean, boolean, character varying"),
            "acknowledge_alarm", Set.of(
                    "uuid, uuid, bigint",
                    "uuid, uuid, uuid, bigint"),
            "clear_alarm", Set.of(
                    "uuid, uuid, bigint, character varying",
                    "uuid, uuid, uuid, bigint, character varying"),
            "assign_alarm", Set.of(
                    "uuid, uuid, uuid, bigint",
                    "uuid, uuid, uuid, uuid, bigint"),
            "unassign_alarm", Set.of(
                    "uuid, uuid, bigint",
                    "uuid, uuid, uuid, bigint"));

    // The five functions exactly as the previous release shipped them, with stub bodies: only the signatures matter
    // here — the replay must drop them by exact signature and re-create both arities.
    private static final String OLD_ARITY_FUNCTIONS = """
            CREATE FUNCTION update_alarm(t_id uuid, a_id uuid, a_severity varchar, a_start_ts bigint, a_end_ts bigint,
                                         a_details varchar,
                                         a_propagate boolean, a_propagate_to_owner boolean, a_propagate_to_owner_hierarchy boolean,
                                         a_propagate_to_tenant boolean, a_propagation_types varchar)
                RETURNS varchar LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$;
            CREATE FUNCTION acknowledge_alarm(t_id uuid, a_id uuid, a_ts bigint)
                RETURNS varchar LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$;
            CREATE FUNCTION clear_alarm(t_id uuid, a_id uuid, a_ts bigint, a_details varchar)
                RETURNS varchar LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$;
            CREATE FUNCTION assign_alarm(t_id uuid, a_id uuid, u_id uuid, a_ts bigint)
                RETURNS varchar LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$;
            CREATE FUNCTION unassign_alarm(t_id uuid, a_id uuid, a_ts bigint)
                RETURNS varchar LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$;
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void replayingSchemaFunctionsOverOldArityFunctionsLeavesBothArities() {
        // Reconstruct the previous release's function set: drop every arity the suite schema may hold, then install
        // the old-arity definitions.
        EXPECTED_ARITIES.forEach((name, arities) ->
                arities.forEach(argTypes -> jdbcTemplate.execute("DROP FUNCTION IF EXISTS " + name + "(" + argTypes + ")")));
        jdbcTemplate.execute(OLD_ARITY_FUNCTIONS);

        // Replay the whole file exactly like SystemPatchApplier's schema-functions replay does (a single execute). This
        // also restores the canonical function set for the rest of the shared suite.
        replaySchemaFunctions();

        // Both arities of every alarm-mutation function are present: the new implementation and the old-arity
        // compatibility wrapper the previous release's nodes keep calling during a rolling upgrade.
        EXPECTED_ARITIES.forEach((name, expected) -> {
            List<String> actual = jdbcTemplate.queryForList(
                    "SELECT oidvectortypes(proargtypes) FROM pg_proc WHERE proname = ?", String.class, name);
            assertEquals("arities of " + name + " after replay", expected, new HashSet<>(actual));
            assertEquals("definitions of " + name + " after replay", expected.size(), actual.size());
        });

        // Replaying over the already-current state (the crash-recovery / re-run case) is a no-op too.
        replaySchemaFunctions();
        EXPECTED_ARITIES.forEach((name, expected) -> {
            List<String> actual = jdbcTemplate.queryForList(
                    "SELECT oidvectortypes(proargtypes) FROM pg_proc WHERE proname = ?", String.class, name);
            assertEquals("arities of " + name + " after second replay", expected, new HashSet<>(actual));
        });
    }

    /**
     * Presence in pg_proc (asserted above) is not enough: the old-arity wrappers must actually RUN against the new
     * schema — resolve the originator by alarm id, delegate to the new-arity implementation, and mutate the row —
     * because that is exactly what a pre-4.3.1.4 node keeps executing during a rolling upgrade. Also pins the
     * unresolvable branch: an unknown alarm id reports {@code success:false} instead of erroring.
     */
    @Test
    public void oldArityWrappersResolveOriginatorAndMutateAgainstTheNewSchema() {
        UUID tenantId = UUID.randomUUID();
        UUID alarmId = UUID.randomUUID();
        UUID originatorId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO alarm (id, created_time, tenant_id, customer_id, originator_id, originator_type, type, " +
                        "severity, start_ts, end_ts, additional_info, propagate, propagate_to_owner, propagate_to_owner_hierarchy, " +
                        "propagate_to_tenant, propagate_relation_types, acknowledged, ack_ts, cleared, clear_ts, assignee_id, assign_ts) " +
                        "VALUES (?, 1000, ?, null, ?, 5, 'SchemaFunctionsReplayTest', 'CRITICAL', 1000, 1000, '{}', " +
                        "false, false, false, false, '', false, 0, false, 0, null, 0)",
                alarmId, tenantId, originatorId);
        try {
            assertOldArityCall(true, "select update_alarm(?, ?, 'MAJOR', 1000, 2000, '{}', false, false, false, false, '')", tenantId, alarmId);
            assertOldArityCall(true, "select acknowledge_alarm(?, ?, 2000)", tenantId, alarmId);
            assertOldArityCall(true, "select assign_alarm(?, ?, ?, 2000)", tenantId, alarmId, assigneeId);
            assertOldArityCall(true, "select unassign_alarm(?, ?, 2000)", tenantId, alarmId);
            assertOldArityCall(true, "select clear_alarm(?, ?, 2000, null)", tenantId, alarmId);

            // The wrappers delegated for real: the new-arity implementations mutated the row.
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT severity, acknowledged, cleared, assignee_id FROM alarm WHERE id = ?", alarmId);
            assertEquals("severity after old-arity update_alarm", "MAJOR", row.get("severity"));
            assertEquals("acknowledged after old-arity acknowledge_alarm", true, row.get("acknowledged"));
            assertEquals("cleared after old-arity clear_alarm", true, row.get("cleared"));
            assertNull("assignee after old-arity unassign_alarm", row.get("assignee_id"));

            // Unknown alarm id: the wrapper cannot resolve an originator and reports failure instead of erroring.
            assertOldArityCall(false, "select acknowledge_alarm(?, ?, 2000)", tenantId, UUID.randomUUID());
        } finally {
            jdbcTemplate.update("DELETE FROM alarm WHERE id = ?", alarmId);
        }
    }

    private void assertOldArityCall(boolean expectedSuccess, String sql, Object... args) {
        String result = jdbcTemplate.queryForObject(sql, String.class, args);
        assertEquals("success of [" + sql + "]", expectedSuccess, JacksonUtil.toJsonNode(result).get("success").asBoolean());
    }

    private void replaySchemaFunctions() {
        try (InputStream in = ResourceUtils.getInputStream(this, "sql/schema-functions.sql")) {
            jdbcTemplate.execute(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to replay schema functions", e);
        }
    }
}
