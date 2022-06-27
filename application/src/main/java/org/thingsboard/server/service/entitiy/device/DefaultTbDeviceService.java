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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbDeviceService extends AbstractTbEntityService implements TbDeviceService {

    @Override
    public Device save(TenantId tenantId, Device device, Device oldDevice, String accessToken, SecurityUser user) throws ThingsboardException {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, accessToken));
            autoCommit(user, savedDevice.getId());
            notificationEntityService.notifyCreateOrUpdateDevice(tenantId, savedDevice.getId(), savedDevice.getCustomerId(),
                    savedDevice, oldDevice, actionType, user);

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), device, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public Device saveDeviceWithCredentials(TenantId tenantId, Device device, DeviceCredentials credentials, SecurityUser user) throws ThingsboardException {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithCredentials(device, credentials));
            notificationEntityService.notifyCreateOrUpdateDevice(tenantId, savedDevice.getId(), savedDevice.getCustomerId(),
                    savedDevice, device, actionType, user);

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), device, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public ListenableFuture<Void> delete(Device device, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(tenantId, deviceId);
            deviceService.deleteDevice(tenantId, deviceId);
            notificationEntityService.notifyDeleteDevice(tenantId, deviceId, device.getCustomerId(), device,
                    relatedEdgeIds, user, deviceId.toString());

            return removeAlarmsByEntityId(tenantId, deviceId);
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    ActionType.DELETED, user, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, Customer customer, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(user.getTenantId(), deviceId, customerId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, deviceId, customerId, savedDevice,
                    actionType, EdgeEventActionType.ASSIGNED_TO_CUSTOMER, user, true, customerId.toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    actionType, user, e, deviceId.toString(), customerId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device unassignDeviceFromCustomer(Device device, Customer customer, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromCustomer(tenantId, deviceId));
            CustomerId customerId = customer.getId();

            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, deviceId, customerId, savedDevice,
                    actionType, EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER, user,
                    true, customerId.toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    actionType, user, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device assignDeviceToPublicCustomer(TenantId tenantId, DeviceId deviceId, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, publicCustomer.getId()));

            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, deviceId, savedDevice.getCustomerId(), savedDevice,
                    actionType, null, user, false, deviceId.toString(),
                    publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    actionType, user, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public DeviceCredentials getDeviceCredentialsByDeviceId(Device device, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.CREDENTIALS_READ;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId));
            notificationEntityService.notifyEntity(tenantId, deviceId, device, device.getCustomerId(),
                    actionType, user, null, deviceId.toString());
            return deviceCredentials;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    actionType, user, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials));
            notificationEntityService.notifyUpdateDeviceCredentials(tenantId, deviceId, device.getCustomerId(), device, result, user);
            return result;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    ActionType.CREDENTIALS_UPDATED, user, e, deviceCredentials);
            throw handleException(e);
        }
    }

    @Override
    public ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, SecurityUser user) throws ThingsboardException {
        try {
            ListenableFuture<ClaimResult> future = claimDevicesService.claimDevice(device, customerId, secretKey);

            return Futures.transform(future, result -> {
                if (result != null && result.getResponse().equals(ClaimResponse.SUCCESS)) {
                    notificationEntityService.notifyEntity(tenantId, device.getId(), result.getDevice(), customerId,
                            ActionType.ASSIGNED_TO_CUSTOMER, user, null, device.getId().toString(), customerId.toString(),
                            customerService.findCustomerById(tenantId, customerId).getName());
                }
                return result;
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, SecurityUser user) throws ThingsboardException {
        try {
            ListenableFuture<ReclaimResult> future = claimDevicesService.reClaimDevice(tenantId, device);

            return Futures.transform(future, result -> {
                Customer unassignedCustomer = result.getUnassignedCustomer();
                if (unassignedCustomer != null) {
                    notificationEntityService.notifyEntity(tenantId, device.getId(), device, device.getCustomerId(), ActionType.UNASSIGNED_FROM_CUSTOMER, user, null,
                            device.getId().toString(), unassignedCustomer.getId().toString(), unassignedCustomer.getName());
                }
                return result;
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public Device assignDeviceToTenant(Device device, Tenant newTenant, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        TenantId newTenantId = newTenant.getId();
        try {
            Tenant tenant = tenantService.findTenantById(tenantId);
            Device assignedDevice = deviceService.assignDeviceToTenant(newTenantId, device);

            notificationEntityService.notifyAssignDeviceToTenant(tenantId, newTenantId, device.getId(),
                    assignedDevice.getCustomerId(), assignedDevice, tenant, user, newTenantId.toString(), newTenant.getName());

            return assignedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    ActionType.ASSIGNED_TO_TENANT, user, e, newTenantId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, Edge edge, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToEdge(tenantId, deviceId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, deviceId, savedDevice.getCustomerId(),
                    edgeId, savedDevice, actionType, user, deviceId.toString(), edgeId.toString(), edge.getName());
            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    actionType, user, e, deviceId.toString(), edgeId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device unassignDeviceFromEdge(Device device, Edge edge, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        EdgeId edgeId = edge.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromEdge(tenantId, deviceId, edgeId));

            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, deviceId, device.getCustomerId(),
                    edgeId, device, actionType, user, deviceId.toString(), edgeId.toString(), edge.getName());
            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    actionType, user, e, deviceId.toString(), edgeId.toString());
            throw handleException(e);
        }
    }

}
