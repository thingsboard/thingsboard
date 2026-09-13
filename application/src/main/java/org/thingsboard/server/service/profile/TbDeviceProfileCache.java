// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.profile;

import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbDeviceProfileCache extends RuleEngineDeviceProfileCache {

    void evict(TenantId tenantId, DeviceProfileId id);

    void evict(TenantId tenantId, DeviceId id);

    DeviceProfile find(DeviceProfileId deviceProfileId);

    DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String deviceType);
}
