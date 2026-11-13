// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.sql.citus.CitusSettings;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared SQL / {@code EXPLAIN} helpers (plus the {@link CitusSettings} test factory) for the Citus tests. The SQL
 * helpers operate purely through a {@link JdbcTemplate} pointed at the coordinator, so they do not depend on any
 * particular test base class and can be reused across the differently-based Citus test classes. The
 * {@code "Task Count:"} parsing in {@link #taskCount} in particular is fiddly to duplicate, hence it is centralised
 * here.
 */
public final class CitusTestSupport {

    private CitusTestSupport() {
    }

    /**
     * Builds a {@link CitusSettings} with the given {@code enabled} flag. In production the settings are populated
     * via {@code @Value} injection, so tests must set the private fields reflectively; the field-name literals live
     * only here, keeping a {@code CitusSettings} field rename a one-place fix.
     */
    public static CitusSettings citusSettings(boolean enabled) {
        CitusSettings settings = new CitusSettings();
        ReflectionTestUtils.setField(settings, "enabled", enabled);
        return settings;
    }

    /** Like {@link #citusSettings(boolean)}, additionally setting the shard count. */
    public static CitusSettings citusSettings(boolean enabled, int shardCount) {
        CitusSettings settings = citusSettings(enabled);
        ReflectionTestUtils.setField(settings, "shardCount", shardCount);
        return settings;
    }

    /**
     * Returns the shard id owning {@code distributionColumnValue} for {@code table} via Citus'
     * {@code get_shard_id_for_distribution_column}.
     */
    public static long shardIdFor(JdbcTemplate jdbcTemplate, String table, UUID distributionColumnValue) {
        Long shardId = jdbcTemplate.queryForObject(
                "select get_shard_id_for_distribution_column(?, ?::uuid)", Long.class, table, distributionColumnValue.toString());
        if (shardId == null) {
            throw new IllegalStateException("get_shard_id_for_distribution_column returned null for " + table);
        }
        return shardId;
    }

    /**
     * Identifies WHICH physical shard owns a given distribution-column value for a table, in a way that is comparable
     * ACROSS co-located tables. Co-located tables do NOT share shard ids -- each table has its own {@code shardid}
     * series -- so equal shard ids is the wrong oracle. The correct co-location oracle is that the owning shards cover
     * the SAME hash range ({@code shardminvalue}/{@code shardmaxvalue}) AND are placed on the SAME worker node
     * ({@code groupid}). Returns "{min}:{max}@{groupid}", so two tables are co-located for that value iff the strings
     * match.
     */
    public static String shardPlacementKey(JdbcTemplate jdbcTemplate, String table, UUID distributionColumnValue) {
        long shardId = shardIdFor(jdbcTemplate, table, distributionColumnValue);
        return jdbcTemplate.queryForObject(
                "select s.shardminvalue || ':' || s.shardmaxvalue || '@' || p.groupid " +
                        "from pg_dist_shard s join pg_dist_placement p on p.shardid = s.shardid " +
                        "where s.shardid = ?", String.class, shardId);
    }

    /**
     * Returns the textual {@code EXPLAIN} output for {@code sql} (one plan line per row, newline-joined).
     */
    public static String explainText(JdbcTemplate jdbcTemplate, String sql) {
        List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : plan) {
            Object value = row.values().iterator().next();
            if (value != null) {
                sb.append(value).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Parses the Citus {@code "Task Count:"} line out of the {@code EXPLAIN} output for {@code sql}. Throws if absent.
     */
    public static int taskCount(JdbcTemplate jdbcTemplate, String sql) {
        String plan = explainText(jdbcTemplate, sql);
        for (String line : plan.split("\n")) {
            int idx = line.indexOf("Task Count:");
            if (idx >= 0) {
                return Integer.parseInt(line.substring(idx + "Task Count:".length()).trim());
            }
        }
        throw new IllegalStateException("Task Count not found in EXPLAIN output: " + plan);
    }
}
