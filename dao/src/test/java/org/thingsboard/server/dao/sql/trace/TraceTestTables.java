// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import com.google.common.io.Resources;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The rule chain tracing and rule engine monitoring tables of schema-entities.sql. Tests that write to them empty them
 * afterwards, so the shared suite DB does not carry their rows into other tests.
 */
public final class TraceTestTables {

    public static final String TABLES =
            "span, trace_summary, trace_group, rule_node_exec_stats, rule_node_queue_stats, rule_node_queue_lag_stats";

    private TraceTestTables() {
    }

    public static void truncate(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("TRUNCATE " + TABLES);
    }

    public static void drop(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + TABLES);
    }

    /**
     * Puts back tables a test dropped; schema-entities.sql is idempotent, as every upgrade re-runs it.
     */
    public static void recreate(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate.queryForObject("SELECT to_regclass('span') IS NOT NULL", Boolean.class)) {
            return;
        }
        try {
            jdbcTemplate.execute(Resources.toString(Resources.getResource("sql/schema-entities.sql"), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
