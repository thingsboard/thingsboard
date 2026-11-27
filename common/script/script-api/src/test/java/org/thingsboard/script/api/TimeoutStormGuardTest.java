// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TimeoutStormGuardTest {

    final UUID a = UUID.randomUUID();
    final UUID b = UUID.randomUUID();
    final UUID c = UUID.randomUUID();
    final UUID d = UUID.randomUUID();

    final TimeoutStormGuard guard = new TimeoutStormGuard("test", 10_000, 3, 60_000);

    @Test
    void singleScriptTimeoutsAreCounted() {
        for (int i = 0; i < 10; i++) {
            assertThat(guard.onTimeout(a, 1000 + i).suppress()).isFalse();
        }
        assertThat(guard.isInStorm(1010)).isFalse();
    }

    @Test
    void twoScriptsAreBelowThreshold() {
        assertThat(guard.onTimeout(a, 1000).suppress()).isFalse();
        assertThat(guard.onTimeout(b, 1001).suppress()).isFalse();
        assertThat(guard.onTimeout(a, 1002).suppress()).isFalse();
    }

    @Test
    void thirdDistinctScriptStartsStormAndReportsStormScripts() {
        guard.onTimeout(a, 1000);
        guard.onTimeout(b, 2000);
        TimeoutStormGuard.Decision decision = guard.onTimeout(c, 3000);

        assertThat(decision.suppress()).isTrue();
        assertThat(decision.stormScripts()).containsExactlyInAnyOrder(a, b, c);
        assertThat(guard.isInStorm(3000)).isTrue();

        TimeoutStormGuard.Decision next = guard.onTimeout(a, 3500);
        assertThat(next.suppress()).isTrue();
        assertThat(next.stormScripts()).isEmpty();
    }

    @Test
    void timeoutsOutsideWindowDoNotStartStorm() {
        guard.onTimeout(a, 1000);
        guard.onTimeout(b, 6000);
        assertThat(guard.onTimeout(c, 12000).suppress()).isFalse(); // 'a' is older than 10 s
        assertThat(guard.onTimeout(d, 13000).suppress()).isTrue(); // b, c, d within 10 s
    }

    @Test
    void stormEndsAfterQuietWindow() {
        guard.onTimeout(a, 1000);
        guard.onTimeout(b, 1001);
        guard.onTimeout(c, 1002);
        assertThat(guard.isInStorm(5000)).isTrue();
        assertThat(guard.isInStorm(11003)).isFalse();

        assertThat(guard.onTimeout(a, 20000).suppress()).isFalse();
    }

    @Test
    void stormIsCappedByMaxDuration() {
        guard.onTimeout(a, 0);
        guard.onTimeout(b, 1);
        guard.onTimeout(c, 2);
        long ts = 2;
        // continuous timeouts keep the storm alive, but only for max duration
        while (ts < 60_000) {
            ts += 5_000;
            assertThat(guard.onTimeout(a, ts).suppress()).as("ts=%s", ts).isEqualTo(ts - 2 <= 60_000);
        }
        assertThat(guard.onTimeout(b, 65_000).suppress()).isFalse();
        assertThat(guard.isInStorm(65_000)).isFalse();

        // a quiet window resets the storm and a new one can start
        guard.onTimeout(a, 80_000);
        guard.onTimeout(b, 80_001);
        assertThat(guard.onTimeout(c, 80_002).suppress()).isTrue();
    }

    @Test
    void minScriptsIsAtLeastTwo() {
        TimeoutStormGuard oneScriptGuard = new TimeoutStormGuard("test", 10_000, 1, 60_000);
        assertThat(oneScriptGuard.onTimeout(a, 1000).suppress()).isFalse();
        assertThat(oneScriptGuard.onTimeout(b, 1001).suppress()).isTrue();
    }

}
