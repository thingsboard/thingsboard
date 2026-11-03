// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import lombok.Data;
import org.thingsboard.server.cache.VersionedCacheKey;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;

@Data
public class DeviceProfileCacheKey implements VersionedCacheKey {

    @Serial
    private static final long serialVersionUID = 8220455917177676472L;

    private final TenantId tenantId;
    private final String name;
    private final DeviceProfileId deviceProfileId;
    private final boolean defaultProfile;
    private final String provisionDeviceKey;

    private DeviceProfileCacheKey(TenantId tenantId, String name, DeviceProfileId deviceProfileId, boolean defaultProfile, String provisionDeviceKey) {
        this.tenantId = tenantId;
        this.name = name;
        this.deviceProfileId = deviceProfileId;
        this.defaultProfile = defaultProfile;
        this.provisionDeviceKey = provisionDeviceKey;
    }

    public static DeviceProfileCacheKey forName(TenantId tenantId, String name) {
        return new DeviceProfileCacheKey(tenantId, name, null, false, null);
    }

    public static DeviceProfileCacheKey forId(DeviceProfileId id) {
        return new DeviceProfileCacheKey(null, null, id, false, null);
    }

    public static DeviceProfileCacheKey forDefaultProfile(TenantId tenantId) {
        return new DeviceProfileCacheKey(tenantId, null, null, true, null);
    }

    public static DeviceProfileCacheKey forProvisionKey(String provisionDeviceKey) {
        return new DeviceProfileCacheKey(null, null, null, false, provisionDeviceKey);
    }

    /**
     * IMPORTANT: Method toString() has to return unique value, if you add additional field to this class, please also refactor toString().
     */
    @Override
    public String toString() {
        if (deviceProfileId != null) {
            return deviceProfileId.toString();
        } else if (defaultProfile) {
            return tenantId.toString();
        } else if (StringUtils.isNotEmpty(provisionDeviceKey)) {
            return provisionDeviceKey;
        }
        return tenantId + "_" + name;
    }

    @Override
    public boolean isVersioned() {
        return deviceProfileId != null;
    }

}
