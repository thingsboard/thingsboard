// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the install-profile split of the {@link CitusDaoConfiguration} locator beans against a real,
 * schema-less Citus coordinator. The install/upgrade context builds its beans BEFORE the schema exists,
 * so the {@code install}-profile variant ({@code citusShardLocatorNoRefresh}) must skip the eager
 * {@code refresh()} — an eager {@code 'attribute_kv'::regclass} probe would throw "relation does not
 * exist" and fail context startup on a fresh database. A companion test proves that premise by showing
 * the {@code !install} eager variant does fail on the same schema-less coordinator, so a regression
 * consolidating the two {@code @Profile} variants back into one eager bean fails here instead of on a
 * real fresh install.
 */
class CitusInstallProfileContextIntegrationTest extends AbstractCitusContainerTest {

    private static final int CONFIGURED_SHARD_COUNT = 7;

    @BeforeEach
    void dropKvSchema() {
        // Guarantee the schema-less premise: a sibling test class sharing the static coordinator may have
        // left the KV tables behind (each such class recreates its own tables in setup, so this is safe).
        jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS ts_kv_latest CASCADE");
    }

    /**
     * Builds an unrefreshed context around the REAL {@link CitusDaoConfiguration} (plus the settings and
     * coordinator {@link JdbcTemplate} beans it needs) with {@code database.citus.enabled=true} and the
     * given active profiles — the closest feasible mirror of the install/upgrade application context.
     */
    private AnnotationConfigApplicationContext contextWithProfiles(String... activeProfiles) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(activeProfiles);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("citusInstallProfileTest", Map.of(
                "database.citus.enabled", "true",
                "database.citus.shard_count", String.valueOf(CONFIGURED_SHARD_COUNT))));
        context.registerBean(JdbcTemplate.class, () -> jdbcTemplate);
        context.register(PropertySourcesPlaceholderConfigurer.class, CitusSettings.class, CitusDaoConfiguration.class);
        return context;
    }

    @Test
    void installProfileContextBootsOnSchemalessDatabaseAndLocatorFallsBackToConfiguredShardCount() {
        try (AnnotationConfigApplicationContext context = contextWithProfiles("install")) {
            assertThatCode(context::refresh)
                    .as("the install-profile locator bean must not eagerly refresh against the missing schema")
                    .doesNotThrowAnyException();

            CitusShardLocator locator = context.getBean(CitusShardLocator.class);
            assertThat(locator.shardCount())
                    .as("the unrefreshed locator must fall back to the configured shard count")
                    .isEqualTo(CONFIGURED_SHARD_COUNT);
        }
    }

    @Test
    void runningServerProfileEagerRefreshFailsOnSchemalessDatabase() {
        // Proves the premise the install variant exists for: outside the install profile the locator bean
        // eagerly refreshes, which cannot work before the schema exists. If this ever stops failing, the
        // no-refresh variant (and the test above) has lost its reason to exist — re-evaluate both.
        try (AnnotationConfigApplicationContext context = contextWithProfiles()) {
            assertThatThrownBy(context::refresh)
                    .isInstanceOf(BeanCreationException.class)
                    .hasStackTraceContaining("attribute_kv");
        }
    }
}
