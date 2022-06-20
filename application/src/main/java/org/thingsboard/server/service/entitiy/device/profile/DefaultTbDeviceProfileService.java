/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.entitiy.device.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.ota.OtaPackageStateService;

import java.util.Objects;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbDeviceProfileService extends AbstractTbEntityService implements TbDeviceProfileService {

    private final DeviceProfileService deviceProfileService;
    private final OtaPackageStateService otaPackageStateService;

    @Override
    public DeviceProfile save(DeviceProfile deviceProfile, User user) throws ThingsboardException {
        ActionType actionType = deviceProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = deviceProfile.getTenantId();
        boolean isFirmwareChanged = false;
        boolean isSoftwareChanged = false;

        if (actionType.equals(ActionType.UPDATED)) {
            DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfile.getId());
            if (!Objects.equals(deviceProfile.getFirmwareId(), oldDeviceProfile.getFirmwareId())) {
                isFirmwareChanged = true;
            }
            if (!Objects.equals(deviceProfile.getSoftwareId(), oldDeviceProfile.getSoftwareId())) {
                isSoftwareChanged = true;
            }
        }
        DeviceProfile savedDeviceProfile = checkNotNull(deviceProfileService.saveDeviceProfile(deviceProfile));

        tbClusterService.onDeviceProfileChange(savedDeviceProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedDeviceProfile.getId(),
                actionType.equals(ActionType.ADDED) ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

        otaPackageStateService.update(savedDeviceProfile, isFirmwareChanged, isSoftwareChanged);

        notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedDeviceProfile.getId(),
                savedDeviceProfile, user, actionType, true, null);
        return savedDeviceProfile;
    }

    @Override
    public void delete(DeviceProfile deviceProfile, User user) {
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        TenantId tenantId = deviceProfile.getTenantId();
        deviceProfileService.deleteDeviceProfile(tenantId, deviceProfileId);

        tbClusterService.onDeviceProfileDelete(deviceProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, deviceProfileId, ComponentLifecycleEvent.DELETED);
        notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, deviceProfileId, deviceProfile,
                user, ActionType.DELETED, true, null, deviceProfileId.toString());
    }

    @Override
    public DeviceProfile setDefaultDeviceProfile(DeviceProfile deviceProfile, DeviceProfile previousDefaultDeviceProfile, User user) throws ThingsboardException {
        TenantId tenantId = deviceProfile.getTenantId();
        if (deviceProfileService.setDefaultDeviceProfile(tenantId, deviceProfile.getId())) {
            if (previousDefaultDeviceProfile != null) {
                previousDefaultDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, previousDefaultDeviceProfile.getId());
                notificationEntityService.logEntityAction(tenantId, previousDefaultDeviceProfile.getId(), previousDefaultDeviceProfile,
                        ActionType.UPDATED, user);
            }
            deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfile.getId());

            notificationEntityService.logEntityAction(tenantId, deviceProfile.getId(), deviceProfile, ActionType.UPDATED, user);
        }
        return deviceProfile;
    }
}
