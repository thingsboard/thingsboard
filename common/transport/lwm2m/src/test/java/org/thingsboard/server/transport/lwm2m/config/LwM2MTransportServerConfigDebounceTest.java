// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
