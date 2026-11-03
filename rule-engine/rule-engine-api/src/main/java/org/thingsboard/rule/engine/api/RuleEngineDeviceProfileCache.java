// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 02.04.18.
 */
public interface RuleEngineDeviceProfileCache {

    DeviceProfile get(TenantId tenantId, DeviceProfileId deviceProfileId);

    DeviceProfile get(TenantId tenantId, DeviceId deviceId);

    void addListener(TenantId tenantId, EntityId listenerId, Consumer<DeviceProfile> profileListener, BiConsumer<DeviceId, DeviceProfile> devicelistener);

    void removeListener(TenantId tenantId, EntityId listenerId);

}
