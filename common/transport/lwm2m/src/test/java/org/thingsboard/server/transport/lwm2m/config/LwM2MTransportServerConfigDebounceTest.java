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
package org.thingsboard.server.transport.lwm2m.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
public class LwM2MTransportServerConfigDebounceTest {

    private static final long DEBOUNCE_SECONDS = (long) ReflectionTestUtils.getField(LwM2MTransportServerConfig.class, "RELOAD_DEBOUNCE_SECONDS");

    @Mock
    private SslCredentialsConfig credentialsConfig;

    @Mock
    private SslCredentialsConfig trustCredentialsConfig;

    private LwM2MTransportServerConfig config;

    @BeforeEach
    public void setup() {
        config = new LwM2MTransportServerConfig();
        ReflectionTestUtils.setField(config, "credentialsConfig", credentialsConfig);
        ReflectionTestUtils.setField(config, "trustCredentialsConfig", trustCredentialsConfig);
    }

    @AfterEach
    public void teardown() {
        config.destroy();
    }

    @Test
    public void givenSingleTrigger_whenScheduleServerReload_thenCallbackFiresOnce() {
        AtomicInteger callCount = new AtomicInteger(0);
        config.registerServerReloadCallback(callCount::incrementAndGet);

        invokeScheduleServerReload();

        await().atMost(DEBOUNCE_SECONDS + 2, SECONDS)
                .untilAsserted(() -> assertThat(callCount.get()).isEqualTo(1));
    }

    @Test
    public void givenTwoRapidTriggers_whenScheduleServerReload_thenCallbackFiresOnce() {
        AtomicInteger callCount = new AtomicInteger(0);
        config.registerServerReloadCallback(callCount::incrementAndGet);

        invokeScheduleServerReload();
        invokeScheduleServerReload();

        await().atMost(DEBOUNCE_SECONDS + 2, SECONDS)
                .untilAsserted(() -> assertThat(callCount.get()).isEqualTo(1));

        // Wait extra to confirm no second invocation
        await().during(DEBOUNCE_SECONDS + 1, SECONDS)
                .atMost(DEBOUNCE_SECONDS + 2, SECONDS)
                .untilAsserted(() -> assertThat(callCount.get()).isEqualTo(1));
    }

    @Test
    public void givenTriggersOutsideDebounceWindow_whenScheduleServerReload_thenCallbackFiresTwice() {
        AtomicInteger callCount = new AtomicInteger(0);
        config.registerServerReloadCallback(callCount::incrementAndGet);

        invokeScheduleServerReload();

        await().atMost(DEBOUNCE_SECONDS + 2, SECONDS)
                .untilAsserted(() -> assertThat(callCount.get()).isEqualTo(1));

        invokeScheduleServerReload();

        await().atMost(DEBOUNCE_SECONDS + 2, SECONDS)
                .untilAsserted(() -> assertThat(callCount.get()).isEqualTo(2));
    }

    private void invokeScheduleServerReload() {
        ReflectionTestUtils.invokeMethod(config, "scheduleServerReload");
    }

}
