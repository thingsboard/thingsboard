// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.common.data.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Replays {@code schema-views.sql} against the test database, the way the upgrade flow replays it through
 * {@code EntityDatabaseSchemaService.createOrUpdateViewsAndFunctions()} once the schema phase is done. A test
 * that reconstructs a pre-upgrade schema needs it: several views are defined as {@code SELECT t.*}, so a column
 * one of them captures can only be dropped with {@code CASCADE}, which takes the view with it.
 */
public final class SchemaViews {

    private SchemaViews() {
    }

    public static void recreate(JdbcTemplate jdbcTemplate) {
        try (InputStream in = ResourceUtils.getInputStream(SchemaViews.class.getClassLoader(), "sql/schema-views.sql")) {
            jdbcTemplate.execute(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to recreate schema views", e);
        }
    }

}
