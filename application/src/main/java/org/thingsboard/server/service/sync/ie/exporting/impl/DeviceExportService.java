/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.sync.ie.DeviceExportData;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class DeviceExportService extends BaseCalculatedFieldsExportService<DeviceId, Device, DeviceExportData> {

    private final DeviceCredentialsService deviceCredentialsService;

    public DeviceExportService(CalculatedFieldService calculatedFieldService, DeviceCredentialsService deviceCredentialsService) {
        super(calculatedFieldService);
        this.deviceCredentialsService = deviceCredentialsService;
    }

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Device device, DeviceExportData exportData) {
        device.setCustomerId(getExternalIdOrElseInternal(ctx, device.getCustomerId()));
        device.setDeviceProfileId(getExternalIdOrElseInternal(ctx, device.getDeviceProfileId()));
        if (ctx.getSettings().isExportCredentials()) {
            var credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(ctx.getTenantId(), device.getId());
            credentials.setId(null);
            credentials.setDeviceId(null);
            exportData.setCredentials(credentials);
        }
        setCalculatedFields(ctx, device, exportData);
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
