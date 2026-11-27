/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.monitoring.service.transport;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class TransportHealthCheckerTest {

    @Test
    void isSessionExpired_durationDisabled_returnsFalseRegardlessOfElapsedTime() {
        TransportHealthChecker<?> checker = mock(TransportHealthChecker.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(checker, "sessionDurationMs", 0L);
        ReflectionTestUtils.setField(checker, "sessionStartTimeNanos", System.nanoTime() - TimeUnit.DAYS.toNanos(1));

        assertThat(checker.isSessionExpired()).isFalse();
    }

    @Test
    void isSessionExpired_beforeDurationElapsed_returnsFalse() {
        TransportHealthChecker<?> checker = mock(TransportHealthChecker.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(checker, "sessionDurationMs", TimeUnit.MINUTES.toMillis(5));
        checker.recordSessionStart();

        assertThat(checker.isSessionExpired()).isFalse();
    }

    @Test
    void isSessionExpired_afterDurationElapsed_returnsTrue() {
        TransportHealthChecker<?> checker = mock(TransportHealthChecker.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(checker, "sessionDurationMs", TimeUnit.SECONDS.toMillis(1));
        ReflectionTestUtils.setField(checker, "sessionStartTimeNanos", System.nanoTime() - TimeUnit.SECONDS.toNanos(10));

        assertThat(checker.isSessionExpired()).isTrue();
    }

    @Test
    void recordSessionStart_recordsCurrentNanoTime() {
        TransportHealthChecker<?> checker = mock(TransportHealthChecker.class, CALLS_REAL_METHODS);

        long before = System.nanoTime();
        checker.recordSessionStart();
        long after = System.nanoTime();

        long recorded = (long) ReflectionTestUtils.getField(checker, "sessionStartTimeNanos");
        assertThat(recorded).isBetween(before, after);
    }

}