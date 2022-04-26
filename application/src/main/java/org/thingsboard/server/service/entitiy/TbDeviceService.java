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
package org.thingsboard.server.service.entitiy;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbDeviceService {

    Device save(SecurityUser user, TenantId tenantId, Device device, Device oldDevice, String accessToken) throws ThingsboardException;

    Device saveDeviceWithCredentials(SecurityUser user, TenantId tenantId, Device device, DeviceCredentials deviceCredentials) throws ThingsboardException;

    void deleteDevice(SecurityUser user, TenantId tenantId, DeviceId deviceId) throws ThingsboardException;

    Device assignDeviceToCustomer(SecurityUser user, TenantId tenantId, DeviceId deviceId, CustomerId customerId) throws ThingsboardException;

    Device unassignDeviceFromCustomer(SecurityUser user, TenantId tenantId, DeviceId deviceId) throws ThingsboardException;

    Device assignDeviceToPublicCustomer(SecurityUser user, TenantId tenantId, DeviceId deviceId) throws ThingsboardException;

    DeviceCredentials getDeviceCredentialsByDeviceId(SecurityUser user, TenantId tenantId, DeviceId deviceId) throws ThingsboardException;

    DeviceCredentials updateDeviceCredentials(SecurityUser user, TenantId tenantId, DeviceCredentials deviceCredentials) throws ThingsboardException;

    Device assignDeviceToTenant(SecurityUser user, TenantId tenantId, TenantId newTenantId, DeviceId deviceId) throws ThingsboardException;

    Device assignDeviceToEdge(SecurityUser user, TenantId tenantId, DeviceId deviceId, EdgeId edgeId) throws ThingsboardException;

    Device unassignDeviceFromEdge(SecurityUser user, TenantId tenantId, DeviceId deviceId, EdgeId edgeId) throws ThingsboardException;
}
