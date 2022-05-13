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
package org.thingsboard.server.service.entitiy.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbDeviceService {

    Device save(TenantId tenantId, Device device, Device oldDevice, String accessToken, SecurityUser user) throws ThingsboardException;

    Device saveDeviceWithCredentials(TenantId tenantId, Device device, DeviceCredentials deviceCredentials, SecurityUser user) throws ThingsboardException;

    ListenableFuture<Void> deleteDevice(Device device, SecurityUser user) throws ThingsboardException;

    Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, Customer customer, SecurityUser user) throws ThingsboardException;

    Device unassignDeviceFromCustomer(Device device, Customer customer, SecurityUser user) throws ThingsboardException;

    Device assignDeviceToPublicCustomer(TenantId tenantId, DeviceId deviceId, SecurityUser user) throws ThingsboardException;

    DeviceCredentials getDeviceCredentialsByDeviceId(Device device, SecurityUser user) throws ThingsboardException;

    DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, SecurityUser user) throws ThingsboardException;

    ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, SecurityUser user) throws ThingsboardException;

    ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, SecurityUser user) throws ThingsboardException;

    Device assignDeviceToTenant(Device device, Tenant newTenant, SecurityUser user) throws ThingsboardException;

    Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, Edge edge, SecurityUser user) throws ThingsboardException;

    Device unassignDeviceFromEdge(Device device, Edge edge, SecurityUser user) throws ThingsboardException;
}
