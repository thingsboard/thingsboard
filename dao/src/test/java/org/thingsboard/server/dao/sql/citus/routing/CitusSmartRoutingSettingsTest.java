// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CitusSmartRoutingSettingsTest {

    private CitusSmartRoutingSettings settingsWithRaw(String raw) {
        CitusSmartRoutingSettings settings = new CitusSmartRoutingSettings();
        ReflectionTestUtils.setField(settings, "workerHostOverridesRaw", raw);
        return settings;
    }

    @Test
    void parsesMultipleEntriesWithWhitespaceAndTrailingComma() {
        Map<String, String> overrides = settingsWithRaw("w1=10.0.0.1:5432, w2=10.0.0.2:5432,").getWorkerHostOverrides();
        assertThat(overrides)
                .hasSize(2)
                .containsEntry("w1", "10.0.0.1:5432")
                .containsEntry("w2", "10.0.0.2:5432");
    }

    @Test
    void parsesSingleEntry() {
        Map<String, String> overrides = settingsWithRaw("worker-1=worker-host:5432").getWorkerHostOverrides();
        assertThat(overrides)
                .hasSize(1)
                .containsEntry("worker-1", "worker-host:5432");
    }

    @Test
    void blankReturnsEmptyMap() {
        assertThat(settingsWithRaw("   ").getWorkerHostOverrides()).isEmpty();
    }

    @Test
    void nullReturnsEmptyMap() {
        assertThat(settingsWithRaw(null).getWorkerHostOverrides()).isEmpty();
    }

    @Test
    void emptyReturnsEmptyMap() {
        assertThat(settingsWithRaw("").getWorkerHostOverrides()).isEmpty();
    }

    @Test
    void skipsBlankEntriesBetweenCommas() {
        Map<String, String> overrides = settingsWithRaw("w1=h1:5432, ,, w2=h2:5432").getWorkerHostOverrides();
        assertThat(overrides)
                .hasSize(2)
                .containsEntry("w1", "h1:5432")
                .containsEntry("w2", "h2:5432");
    }

    @Test
    void skipsMalformedEntriesWithoutEquals() {
        Map<String, String> overrides = settingsWithRaw("w1=h1:5432, garbage, w2=h2:5432").getWorkerHostOverrides();
        assertThat(overrides)
                .hasSize(2)
                .containsEntry("w1", "h1:5432")
                .containsEntry("w2", "h2:5432");
    }

    @Test
    void skipsEntriesWithBlankKey() {
        Map<String, String> overrides = settingsWithRaw("=h1:5432, w2=h2:5432").getWorkerHostOverrides();
        assertThat(overrides)
                .hasSize(1)
                .containsEntry("w2", "h2:5432");
    }

    @Test
    void keepsValueWithMultipleColonsAsIs() {
        Map<String, String> overrides = settingsWithRaw("w1=[::1]:5432").getWorkerHostOverrides();
        assertThat(overrides).containsEntry("w1", "[::1]:5432");
    }

    // --- validate(): the @PostConstruct fail-fast guard for Hikari's 250ms validationTimeout floor ---

    @Test
    void validateRejectsWorkerConnectionTimeoutBelowTheFloor() {
        CitusSmartRoutingSettings settings = new CitusSmartRoutingSettings();
        ReflectionTestUtils.setField(settings, "workerConnectionTimeoutMs",
                CitusSmartRoutingSettings.MIN_WORKER_CONNECTION_TIMEOUT_MS - 1);

        assertThatThrownBy(settings::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database.citus.smart_routing.worker_connection_timeout_ms");
    }

    @Test
    void validatePassesAtTheFloor() {
        CitusSmartRoutingSettings settings = new CitusSmartRoutingSettings();
        ReflectionTestUtils.setField(settings, "workerConnectionTimeoutMs",
                CitusSmartRoutingSettings.MIN_WORKER_CONNECTION_TIMEOUT_MS);

        assertThatCode(settings::validate).doesNotThrowAnyException();
    }
}
