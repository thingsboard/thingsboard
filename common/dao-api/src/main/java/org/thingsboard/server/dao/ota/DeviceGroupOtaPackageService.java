// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.ota;

import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;

import java.util.List;
import java.util.UUID;

public interface DeviceGroupOtaPackageService {

    DeviceGroupOtaPackage findDeviceGroupOtaPackageById(UUID id);

    DeviceGroupOtaPackage findDeviceGroupOtaPackageByGroupIdAndType(EntityGroupId groupId, OtaPackageType type);

    List<DeviceGroupOtaPackage> findDeviceGroupOtaPackageByGroupId(EntityGroupId groupId);

    DeviceGroupOtaPackage saveDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage);

    void deleteDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage);

}
