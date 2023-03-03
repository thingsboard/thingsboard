/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.DeviceExportData;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceImportService extends BaseEntityImportService<DeviceId, Device, DeviceExportData> {

    private final DeviceService deviceService;
    private final DeviceCredentialsService credentialsService;

    @Override
    protected void setOwner(TenantId tenantId, Device device, IdProvider idProvider) {
        device.setTenantId(tenantId);
        device.setCustomerId(idProvider.getInternalId(device.getCustomerId()));
    }

    @Override
    protected Device prepare(EntitiesImportCtx ctx, Device device, Device old, DeviceExportData exportData, IdProvider idProvider) {
        device.setDeviceProfileId(idProvider.getInternalId(device.getDeviceProfileId()));
        device.setFirmwareId(getOldEntityField(old, Device::getFirmwareId));
        device.setSoftwareId(getOldEntityField(old, Device::getSoftwareId));
        return device;
    }

    @Override
    protected Device deepCopy(Device d) {
        return new Device(d);
    }

    @Override
    protected void cleanupForComparison(Device e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @Override
    protected Device saveOrUpdate(EntitiesImportCtx ctx, Device device, DeviceExportData exportData, IdProvider idProvider) {
        if (exportData.getCredentials() != null && ctx.isSaveCredentials()) {
            exportData.getCredentials().setId(null);
            exportData.getCredentials().setDeviceId(null);
            return deviceService.saveDeviceWithCredentials(device, exportData.getCredentials());
        } else {
            return deviceService.saveDevice(device);
        }
    }

    @Override
    protected boolean updateRelatedEntitiesIfUnmodified(EntitiesImportCtx ctx, Device prepared, DeviceExportData exportData, IdProvider idProvider) {
        boolean updated = super.updateRelatedEntitiesIfUnmodified(ctx, prepared, exportData, idProvider);
        var credentials = exportData.getCredentials();
        if (credentials != null && ctx.isSaveCredentials()) {
            var existing = credentialsService.findDeviceCredentialsByDeviceId(ctx.getTenantId(), prepared.getId());
            credentials.setId(existing.getId());
            credentials.setDeviceId(prepared.getId());
            if (!existing.equals(credentials)) {
                credentialsService.updateDeviceCredentials(ctx.getTenantId(), credentials);
                updated = true;
            }
        }
        return updated;
    }

    @Override
    protected void onEntitySaved(User user, Device savedDevice, Device oldDevice) throws ThingsboardException {
        entityNotificationService.notifyCreateOrUpdateDevice(user.getTenantId(), savedDevice.getId(), savedDevice.getCustomerId(),
                savedDevice, oldDevice, oldDevice == null ? ActionType.ADDED : ActionType.UPDATED, user);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
