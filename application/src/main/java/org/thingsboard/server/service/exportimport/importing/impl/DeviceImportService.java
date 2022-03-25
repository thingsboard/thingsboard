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
package org.thingsboard.server.service.exportimport.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.exportimport.exporting.data.DeviceExportData;
import org.thingsboard.server.service.exportimport.importing.EntityImportSettings;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceImportService extends AbstractEntityImportService<DeviceId, Device, DeviceExportData> {

    private final DeviceService deviceService;


    @Override
    protected Device prepareAndSaveEntity(TenantId tenantId, Device device, Device existingDevice, DeviceExportData exportData, EntityImportSettings importSettings) {
        device.setCustomerId(getInternalId(tenantId, device.getCustomerId()));
        device.setDeviceProfileId(getInternalId(tenantId, device.getDeviceProfileId()));
        device.setFirmwareId(getInternalId(tenantId, device.getFirmwareId()));
        device.setSoftwareId(getInternalId(tenantId, device.getSoftwareId()));

        return deviceService.saveDeviceWithCredentials(device, exportData.getCredentials());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
