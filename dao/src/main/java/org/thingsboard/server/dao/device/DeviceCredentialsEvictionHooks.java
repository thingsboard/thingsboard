// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
final class DeviceCredentialsEvictionHooks {

    private DeviceCredentialsEvictionHooks() {
    }

    /**
     * Invokes every hook, fail-open per hook. Never logs the event itself: credentials ids are secrets (access tokens).
     */
    static void invokeAll(List<DeviceCredentialsEvictionHook> hooks, DeviceCredentialsEvictEvent event) {
        for (DeviceCredentialsEvictionHook hook : hooks) {
            try {
                hook.onDeviceCredentialsEvict(event);
            } catch (Exception e) {
                log.warn("Device credentials eviction hook [{}] failed on credentials change (deleted: {})",
                        hook.getClass().getSimpleName(), event.isDeleted(), e);
            }
        }
    }

}
