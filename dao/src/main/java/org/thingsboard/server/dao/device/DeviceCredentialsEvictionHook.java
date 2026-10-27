// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

/**
 * Invoked whenever device credentials were actually created, rotated or deleted
 * (after commit in transactional flows). Never invoked for defensive evictions on failed saves.
 */
public interface DeviceCredentialsEvictionHook {

    void onDeviceCredentialsEvict(DeviceCredentialsEvictEvent event);

}
