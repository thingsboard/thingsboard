// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.ota;

import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.UUID;

public interface DeviceGroupOtaPackageDao extends TenantEntityDao<DeviceGroupOtaPackage> {

    DeviceGroupOtaPackage findDeviceGroupOtaPackageById(UUID id);

    DeviceGroupOtaPackage findDeviceGroupOtaPackageByGroupIdAndType(UUID groupId, OtaPackageType type);

    List<DeviceGroupOtaPackage> findDeviceGroupOtaPackageByGroupId(UUID groupId);

    DeviceGroupOtaPackage saveDeviceGroupOtaPackage(DeviceGroupOtaPackage deviceGroupOtaPackage);

    boolean deleteDeviceGroupOtaPackage(UUID id);

}
