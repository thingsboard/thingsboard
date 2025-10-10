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
package org.thingsboard.server.service.entitiy.device;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbDeviceService extends AbstractTbEntityService implements TbDeviceService {

    private final DeviceService deviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final ClaimDevicesService claimDevicesService;

    @Override
    public Device save(Device device, String accessToken, User user) throws Exception {
        return save(device, accessToken, NameConflictStrategy.DEFAULT, user);
    }

    @Override
    public Device save(Device device, String accessToken, NameConflictStrategy nameConflictStrategy, User user) throws Exception {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, accessToken, nameConflictStrategy));
            autoCommit(user, savedDevice.getId());
            logEntityActionService.logEntityAction(tenantId, savedDevice.getId(), savedDevice, savedDevice.getCustomerId(),
                    actionType, user);

            return savedDevice;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), device, actionType, user, e);
            throw e;
        }
    }

    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials credentials, User user) throws ThingsboardException {
        return saveDeviceWithCredentials(device, credentials, NameConflictStrategy.DEFAULT, user);
    }

    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials credentials, NameConflictStrategy nameConflictStrategy, User user) throws ThingsboardException {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithCredentials(device, credentials, nameConflictStrategy));
            logEntityActionService.logEntityAction(tenantId, savedDevice.getId(), savedDevice, savedDevice.getCustomerId(),
                    actionType, user);

            return savedDevice;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), device, actionType, user, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void delete(Device device, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            deviceService.deleteDevice(tenantId, deviceId);
            logEntityActionService.logEntityAction(tenantId, deviceId, device, device.getCustomerId(), actionType,
                    user, deviceId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    @Override
    public Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, customerId));
            logEntityActionService.logEntityAction(tenantId, deviceId, savedDevice, customerId, actionType, user,
                    deviceId.toString(), customerId.toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType, user,
                    e, deviceId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public Device unassignDeviceFromCustomer(Device device, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromCustomer(tenantId, deviceId));
            CustomerId customerId = customer.getId();

            logEntityActionService.logEntityAction(tenantId, deviceId, savedDevice, customerId, actionType, user,
                    deviceId.toString(), customerId.toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    @Override
    public Device assignDeviceToPublicCustomer(TenantId tenantId, DeviceId deviceId, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, publicCustomer.getId()));

            logEntityActionService.logEntityAction(tenantId, deviceId, savedDevice, savedDevice.getCustomerId(),
                    actionType, user, deviceId.toString(), publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    @Override
    public DeviceCredentials getDeviceCredentialsByDeviceId(Device device, User user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId));
            logEntityActionService.logEntityAction(tenantId, deviceId, device, device.getCustomerId(),
                    ActionType.CREDENTIALS_READ, user, deviceId.toString());
            return deviceCredentials;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    ActionType.CREDENTIALS_READ, user, e, deviceId.toString());
            throw e;
        }
    }

    @Override
    public DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException {
        ActionType actionType = ActionType.CREDENTIALS_UPDATED;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials));
            logEntityActionService.logEntityAction(tenantId, deviceId, device, device.getCustomerId(),
                    actionType, user, result);
            return result;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    actionType, user, e, deviceCredentials);
            throw e;
        }
    }

    @Override
    public ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, User user) {
        ListenableFuture<ClaimResult> future = claimDevicesService.claimDevice(device, customerId, secretKey);

        return Futures.transform(future, result -> {
            if (result != null && result.getResponse().equals(ClaimResponse.SUCCESS)) {
                logEntityActionService.logEntityAction(tenantId, device.getId(), result.getDevice(), customerId,
                        ActionType.ASSIGNED_TO_CUSTOMER, user, device.getId().toString(), customerId.toString(),
                        customerService.findCustomerById(tenantId, customerId).getName());
            }
            return result;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, User user) {
        ListenableFuture<ReclaimResult> future = claimDevicesService.reClaimDevice(tenantId, device);

        return Futures.transform(future, result -> {
            Customer unassignedCustomer = result.getUnassignedCustomer();
            if (unassignedCustomer != null) {
                logEntityActionService.logEntityAction(tenantId, device.getId(), device, device.getCustomerId(),
                        ActionType.UNASSIGNED_FROM_CUSTOMER, user, device.getId().toString(),
                        unassignedCustomer.getId().toString(), unassignedCustomer.getName());
            }
            return result;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public Device assignDeviceToTenant(Device device, Tenant newTenant, User user) {
        ActionType actionType = ActionType.ASSIGNED_TO_TENANT;
        TenantId tenantId = device.getTenantId();
        TenantId newTenantId = newTenant.getId();
        DeviceId deviceId = device.getId();
        try {
            Device assignedDevice = deviceService.assignDeviceToTenant(newTenantId, device);

            logEntityActionService.logEntityAction(tenantId, deviceId, assignedDevice, assignedDevice.getCustomerId(),
                    actionType, user, newTenantId.toString(), newTenant.getName());

            return assignedDevice;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    actionType, user, e, deviceId.toString());
            throw e;
        }
    }

    @Override
    public Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToEdge(tenantId, deviceId, edgeId));
            logEntityActionService.logEntityAction(tenantId, deviceId, savedDevice, savedDevice.getCustomerId(),
                    actionType, user, deviceId.toString(), edgeId.toString(), edge.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    actionType, user, e, deviceId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    public Device unassignDeviceFromEdge(Device device, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        EdgeId edgeId = edge.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromEdge(tenantId, deviceId, edgeId));
            logEntityActionService.logEntityAction(tenantId, deviceId, savedDevice, savedDevice.getCustomerId(),
                    actionType, user, deviceId.toString(), edgeId.toString(), edge.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    actionType, user, e, deviceId.toString(), edgeId.toString());
            throw e;
        }
    }

}
