// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CitusSettingsTest {

    @Test
    void disabledByDefault() {
        CitusSettings settings = new CitusSettings();
        assertThat(settings.isEnabled()).isFalse();
    }

    @Test
    void exposesConfiguredShardCount() {
        CitusSettings settings = new CitusSettings();
        ReflectionTestUtils.setField(settings, "enabled", true);
        ReflectionTestUtils.setField(settings, "shardCount", 64);
        assertThat(settings.isEnabled()).isTrue();
        assertThat(settings.getShardCount()).isEqualTo(64);
    }

    @Test
    void defaultShardCountIs32() {
        // Behavioral test: bind the bean in a context with NO database.citus.shard_count property set and
        // assert the @Value default actually resolves to 32 (not just that the literal string matches).
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(PropertySourcesPlaceholderConfigurer.class);
            ctx.register(CitusSettings.class);
            ctx.refresh();
            CitusSettings settings = ctx.getBean(CitusSettings.class);
            assertThat(settings.getShardCount()).isEqualTo(32);
        }
    }
}
