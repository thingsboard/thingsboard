// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.sync.ie.DeviceExportData;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceExportService extends BaseEntityExportService<DeviceId, Device, DeviceExportData> {

    private final DeviceCredentialsService deviceCredentialsService;

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Device device, DeviceExportData exportData) {
        device.setCustomerId(getExternalIdOrElseInternal(ctx, device.getCustomerId()));
        device.setDeviceProfileId(getExternalIdOrElseInternal(ctx, device.getDeviceProfileId()));
        device.setFirmwareId(getExternalIdOrElseInternal(ctx, device.getFirmwareId()));
        device.setSoftwareId(getExternalIdOrElseInternal(ctx, device.getSoftwareId()));
        if (ctx.getSettings().isExportCredentials()) {
            var credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(ctx.getTenantId(), device.getId());
            credentials.setId(null);
            credentials.setDeviceId(null);
            exportData.setCredentials(credentials);
        }
    }

    @Override
    protected DeviceExportData newExportData() {
        return new DeviceExportData();
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.DEVICE);
    }

}
