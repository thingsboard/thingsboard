// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;

class DevicesGroupAllUpdater extends EntityGroupAllPaginatedUpdater<DeviceId, Device> {

    private final DeviceService deviceService;

    public DevicesGroupAllUpdater(DeviceService deviceService, CustomerService customerService,
                                  EntityGroupService entityGroupService, EntityGroup groupAll, boolean fetchAllTenantEntities) {
        super(customerService,
                entityGroupService,
                groupAll,
                fetchAllTenantEntities,
                deviceService::findDevicesByTenantId,
                deviceService::findDevicesByTenantIdAndIdsAsync,
                entityId -> new DeviceId(entityId.getId()),
                Device::getId);
        this.deviceService = deviceService;
    }

    @Override
    protected void unassignFromCustomer(Device entity) {
        entity.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        deviceService.saveDevice(entity);
    }

    @Override
    protected String getName() {
        return "Devices group all updater";
    }
}
