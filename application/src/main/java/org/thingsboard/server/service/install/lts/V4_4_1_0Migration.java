// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_JDBC_TEMPLATE;

/**
 * {@link MigrationService} selects migrations from the injected {@link Migration} beans, not from the
 * on-disk {@code data/upgrade/lts/<version>/} directories. So this bean is what makes the runner discover
 * version {@code 4.4.1.0} and execute its {@code data/upgrade/lts/4.4.1.0/schema_update.sql}, which adds the
 * {@code idx_calculated_field_tenant_id_id} index used by tenant-scoped calculated field paging and the
 * {@code trace_coverage_setting} table.
 * <p>
 * {@link #apply()} creates the trace tables from {@code sql/schema-trace.sql}. They are not in
 * {@code schema_update.sql} because the trace DAOs use the dedicated events database when
 * {@code spring.datasource.events.enabled} is set, and {@code schema_update.sql} always runs on the main one.
 * The script is idempotent, so the offline upgrade path, which already ran it from {@code ThingsboardInstallService},
 * re-runs it as a no-op.
 * <p>
 * It needs a slot of its own rather than {@code 4.4.0.0}: that version is already released, and
 * {@link Version#isInRange} is exclusive at the lower bound, so an installation already on {@code 4.4.0.0}
 * would never pick up a change added there.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code MigrationIntegrationTest}.
 */
@Slf4j
@Component
@TbCoreComponent
public class V4_4_1_0Migration implements Migration {

    static final String SCHEMA_TRACE_SQL = "sql/schema-trace.sql";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<JdbcTemplate> eventsJdbcTemplate;

    public V4_4_1_0Migration(JdbcTemplate jdbcTemplate,
                             @Qualifier(EVENTS_JDBC_TEMPLATE) ObjectProvider<JdbcTemplate> eventsJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventsJdbcTemplate = eventsJdbcTemplate;
    }

    @Override
    public String getVersion() {
        return "4.4.1.0";
    }

    /**
     * On the main database this joins the schema transaction; on a dedicated events database it commits on its own,
     * which is safe because the script is idempotent and a failure leaves the version unrecorded for a retry.
     */
    @Override
    public void apply() {
        String sql;
        try {
            sql = Resources.toString(Resources.getResource(SCHEMA_TRACE_SQL), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + SCHEMA_TRACE_SQL, e);
        }
        eventsJdbcTemplate.getIfAvailable(() -> jdbcTemplate).execute(sql);
        log.info("Created trace tables from {}", SCHEMA_TRACE_SQL);
    }
}
