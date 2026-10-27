// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.dao.util.LogCapture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeviceCredentialsEvictionHooksTest {

    @Test
    void givenFailingHook_whenInvokeAll_thenOtherHooksStillInvokedAndCredentialsIdNotLogged() {
        DeviceCredentialsEvictionHook failing = mock(DeviceCredentialsEvictionHook.class);
        DeviceCredentialsEvictionHook healthy = mock(DeviceCredentialsEvictionHook.class);
        doThrow(new RuntimeException("cache down")).when(failing).onDeviceCredentialsEvict(any());
        DeviceCredentialsEvictEvent event = new DeviceCredentialsEvictEvent("secret-new-token", "secret-old-token");

        LogCapture logs = new LogCapture(DeviceCredentialsEvictionHooks.class, Level.WARN);
        try {
            assertThatCode(() -> DeviceCredentialsEvictionHooks.invokeAll(List.of(failing, healthy), event)).doesNotThrowAnyException();

            verify(failing).onDeviceCredentialsEvict(event);
            verify(healthy).onDeviceCredentialsEvict(event);
            assertThat(logs.getMessages()).hasSize(1);
            assertThat(logs.getMessages().get(0)).doesNotContain("secret-new-token").doesNotContain("secret-old-token");
        } finally {
            logs.detach();
        }
    }

    @Test
    void givenNoHooks_whenInvokeAll_thenNoop() {
        assertThatCode(() -> DeviceCredentialsEvictionHooks.invokeAll(List.of(), new DeviceCredentialsEvictEvent("a", null)))
                .doesNotThrowAnyException();
    }

}
