/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;

import java.util.Optional;

public interface DeviceService {
    
    Device findDeviceById(DeviceId deviceId);

    Optional<Device> findDeviceByTenantIdAndName(TenantId tenantId, String name);

    Device saveDevice(Device device);

    Device assignDeviceToCustomer(DeviceId deviceId, CustomerId customerId);

    Device unassignDeviceFromCustomer(DeviceId deviceId);

    void deleteDevice(DeviceId deviceId);

    TextPageData<Device> findDevicesByTenantId(TenantId tenantId, TextPageLink pageLink);

    void deleteDevicesByTenantId(TenantId tenantId);

    TextPageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink);

    void unassignCustomerDevices(TenantId tenantId, CustomerId customerId);
}
