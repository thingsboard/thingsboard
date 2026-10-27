// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
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
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbDeviceService extends AbstractTbEntityService implements TbDeviceService {

    private final DeviceService deviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final ClaimDevicesService claimDevicesService;
    private final OwnersCacheService ownersCacheService;

    @Override
    public Device save(Device device, EntityGroup entityGroup) throws Exception {
        return save(device, null, entityGroup, null);
    }

    @Override
    public Device save(Device device, String accessToken, EntityGroup entityGroup, User user) throws Exception {
        return save(device, accessToken, entityGroup != null ? Collections.singletonList(entityGroup) : null, user);
    }

    @Override
    public Device save(Device device, String accessToken, List<EntityGroup> entityGroups, User user) throws Exception {
        return save(device, accessToken, entityGroups, NameConflictStrategy.DEFAULT, user);
    }

    @Override
    public Device save(Device device, String accessToken, List<EntityGroup> entityGroups, NameConflictStrategy nameConflictStrategy, User user) throws Exception {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, accessToken, nameConflictStrategy));
        autoCommit(user, savedDevice.getId());
        createOrUpdateGroupEntity(tenantId, savedDevice, entityGroups, actionType, user);
        return savedDevice;
    }

    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials credentials, EntityGroup entityGroup, User user) throws ThingsboardException {
        return saveDeviceWithCredentials(device, credentials, entityGroup != null ? Collections.singletonList(entityGroup) : null, user);
    }

    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials credentials, List<EntityGroup> entityGroups, User user) throws ThingsboardException {
        return saveDeviceWithCredentials(device, credentials, entityGroups, NameConflictStrategy.DEFAULT, user);
    }

    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials credentials, List<EntityGroup> entityGroups, NameConflictStrategy nameConflictStrategy, User user) throws ThingsboardException {
        boolean isCreate = device.getId() == null;
        ActionType actionType = isCreate ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithCredentials(device, credentials, nameConflictStrategy));
            createOrUpdateGroupEntity(tenantId, savedDevice, entityGroups, actionType, user);
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
    public void delete(DeviceId deviceId, User user) {
        Device device = deviceService.findDeviceById(user.getTenantId(), deviceId);
        delete(device, user);
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
    public ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId
            customerId, String secretKey, User user) {
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
            ownersCacheService.clearOwners(deviceId);

            logEntityActionService.logEntityAction(tenantId, deviceId, assignedDevice, assignedDevice.getCustomerId(),
                    actionType, user, newTenantId.toString(), newTenant.getName());

            return assignedDevice;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    actionType, user, e, deviceId.toString());
            throw e;
        }
    }

}
