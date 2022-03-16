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
package org.thingsboard.server.service.expimp.imp.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.export.impl.DeviceExportData;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.expimp.imp.EntityImportResult;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceImportService extends AbstractEntityImportService<DeviceId, Device, DeviceExportData> {

    private final DeviceService deviceService;


    @Transactional
    @Override
    public EntityImportResult<Device> importEntity(TenantId tenantId, DeviceExportData exportData) {
        Device device = exportData.getDevice();
        Device existingDevice = findByExternalId(tenantId, device.getId()); // FIXME: !!!
        // what if exporting and importing back already exported entity ? (save version and then load it back)
        /*
         * export entity -> id from env1 -> import this entity -> ...
         *
         * maybe find not only by external id but by internal too ? but then what if we will try
         * */

        device.setExternalId(device.getId());
        device.setTenantId(tenantId);

        if (existingDevice == null) {
            device.setId(null);
            device.setCustomerId(null); // FIXME: find and set customer
        } else {
            device.setId(existingDevice.getId());
            device.setCustomerId(existingDevice.getCustomerId());
        }

        // TODO or maybe set as additional config whether to update related entities when device already exists ?
        // TODO: or also whether to ignore not found internal ids

        // FIXME: review use cases for version controlling: in the same tenant, between tenants, between environments and different tenants

        device.setDeviceProfileId(getInternalId(tenantId, device.getDeviceProfileId()));
        device.setFirmwareId(getInternalId(tenantId, device.getFirmwareId()));
        device.setSoftwareId(getInternalId(tenantId, device.getSoftwareId()));

        Device savedDevice = deviceService.saveDeviceWithCredentials(device, exportData.getCredentials());

        EntityImportResult<Device> importResult = new EntityImportResult<>();
        importResult.setSavedEntity(savedDevice);
        importResult.setOldEntity(existingDevice);
        return importResult;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
