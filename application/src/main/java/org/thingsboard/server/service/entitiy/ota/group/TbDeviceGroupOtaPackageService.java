// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.ota.group;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;

public interface TbDeviceGroupOtaPackageService {

    DeviceGroupOtaPackage saveDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage, User User) throws Exception;

    void deleteDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage);

}
