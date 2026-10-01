// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TbAiChannelAvailabilityTest {

    @Test
    void shouldBeUnusable_whenDisabled() {
        assertThat(new TbAiChannelAvailability(false, Duration.ofMinutes(10), Clock.systemUTC()).isUsable()).isFalse();
    }

    @Test
    void shouldBackOffAndRecover_whenMarkedUnavailable() {
        // GIVEN
        var clock = new TbAiTransportSelectorTest.MutableClock(Instant.parse("2026-10-01T10:00:00Z"));
        var availability = new TbAiChannelAvailability(true, Duration.ofMinutes(10), clock);
        assertThat(availability.isUsable()).isTrue();

        // WHEN
        availability.markUnavailable();

        // THEN
        assertThat(availability.isUsable()).isFalse();
        clock.advance(Duration.ofMinutes(9));
        assertThat(availability.isUsable()).isFalse();
        clock.advance(Duration.ofMinutes(2));
        assertThat(availability.isUsable()).isTrue();
    }

}
