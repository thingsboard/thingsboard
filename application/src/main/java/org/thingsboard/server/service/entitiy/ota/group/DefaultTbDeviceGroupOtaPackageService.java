// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.ota.group;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.dao.ota.OtaPackageStateService;

import java.util.Optional;

@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbDeviceGroupOtaPackageService extends AbstractTbEntityService implements TbDeviceGroupOtaPackageService {

    private final OtaPackageStateService otaPackageStateService;
    private final DeviceGroupOtaPackageService deviceGroupOtaPackageService;

    @Override
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage, User user) throws Exception {
        DeviceGroupOtaPackage oldDeviceGroupOtaPackage = Optional.ofNullable(deviceGroupOtaPackage.getId())
                .map(deviceGroupOtaPackageService::findDeviceGroupOtaPackageById).orElse(null);

        DeviceGroupOtaPackage savedDeviceGroupOtaPackage = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        otaPackageStateService.update(tenantId, savedDeviceGroupOtaPackage, oldDeviceGroupOtaPackage);
        autoCommit(user, EntityType.DEVICE, savedDeviceGroupOtaPackage.getGroupId());
        return savedDeviceGroupOtaPackage;
    }

    @Override
    public void deleteDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage) {
        deviceGroupOtaPackageService.deleteDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        otaPackageStateService.update(tenantId, null, deviceGroupOtaPackage);
    }

}
