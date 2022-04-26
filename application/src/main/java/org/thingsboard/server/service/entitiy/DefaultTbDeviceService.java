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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.msg.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
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
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbDeviceService extends AbstractTbEntityService implements TbDeviceService {

    private final GatewayNotificationsService gatewayNotificationsService;

    @Override
    public Device save(SecurityUser user, TenantId tenantId, Device device, Device oldDevice, String accessToken) throws ThingsboardException {
        boolean created = device.getId() == null;
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, accessToken));
            tbClusterService.onDeviceUpdated(savedDevice, oldDevice);

            logEntityAction(user, tenantId, savedDevice.getId(), savedDevice,
                    savedDevice.getCustomerId(),
                    created ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), device,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @Override
    public Device saveDeviceWithCredentials(SecurityUser user, TenantId tenantId, Device device, DeviceCredentials credentials) throws ThingsboardException {
        boolean created = device.getId() == null;
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithCredentials(device, credentials));
            tbClusterService.onDeviceUpdated(savedDevice, device);
            logEntityAction(user, tenantId, savedDevice.getId(), savedDevice,
                    savedDevice.getCustomerId(),
                    created ? ActionType.ADDED : ActionType.UPDATED, null);
            return savedDevice;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), device,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @Override
    public void deleteDevice(SecurityUser user, TenantId tenantId, DeviceId deviceId) throws ThingsboardException {
        try {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(tenantId, deviceId);

            deviceService.deleteDevice(tenantId, deviceId);

            gatewayNotificationsService.onDeviceDeleted(device);
            tbClusterService.onDeviceDeleted(device, null);

            logEntityAction(user, tenantId, deviceId, device,
                    device.getCustomerId(),
                    ActionType.DELETED, null, deviceId.toString());

            sendDeleteNotificationMsg(tenantId, deviceId, relatedEdgeIds);
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE),
                    null,
                    null,
                    ActionType.DELETED, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device assignDeviceToCustomer(SecurityUser user, TenantId tenantId, DeviceId deviceId, CustomerId customerId) throws ThingsboardException {
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(user.getTenantId(), deviceId, customerId));

            Customer customer = customerService.findCustomerById(user.getTenantId(), customerId);

            logEntityAction(user, tenantId, deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, deviceId.toString(), customerId.toString(), customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedDevice.getTenantId(), savedDevice.getId(),
                    customerId, EdgeEventActionType.ASSIGNED_TO_CUSTOMER);

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, deviceId.toString(), customerId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device unassignDeviceFromCustomer(SecurityUser user, TenantId tenantId, DeviceId deviceId) throws ThingsboardException {
        try {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            Customer customer = customerService.findCustomerById(tenantId, device.getCustomerId());
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromCustomer(tenantId, deviceId));

            logEntityAction(user, tenantId, deviceId, device,
                    device.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, deviceId.toString(), customer.getId().toString(), customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedDevice.getTenantId(), savedDevice.getId(),
                    customer.getId(), EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER);

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device assignDeviceToPublicCustomer(SecurityUser user, TenantId tenantId, DeviceId deviceId) throws ThingsboardException {
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, publicCustomer.getId()));

            logEntityAction(user, tenantId, deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, deviceId.toString(), publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public DeviceCredentials getDeviceCredentialsByDeviceId(SecurityUser user, TenantId tenantId, DeviceId deviceId) throws ThingsboardException {
        try {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId));
            logEntityAction(user, tenantId, deviceId, device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_READ, null, deviceId.toString());
            return deviceCredentials;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_READ, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public DeviceCredentials updateDeviceCredentials(SecurityUser user, TenantId tenantId, DeviceCredentials deviceCredentials) throws ThingsboardException {
        try {
            DeviceId deviceId = deviceCredentials.getDeviceId();
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials));
            tbClusterService.pushMsgToCore(new DeviceCredentialsUpdateNotificationMsg(tenantId, deviceCredentials.getDeviceId(), result), null);

            sendEntityNotificationMsg(tenantId, device.getId(), EdgeEventActionType.CREDENTIALS_UPDATED);

            logEntityAction(user, tenantId, deviceId, device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_UPDATED, null, deviceCredentials);
            return result;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_UPDATED, e, deviceCredentials);
            throw handleException(e);
        }
    }

    @Override
    public Device assignDeviceToTenant(SecurityUser user, TenantId tenantId, TenantId newTenantId, DeviceId deviceId) throws ThingsboardException {
        try {
            Tenant newTenant = tenantService.findTenantById(newTenantId);
            Device device = deviceService.findDeviceById(tenantId, deviceId);

            Device assignedDevice = deviceService.assignDeviceToTenant(newTenantId, device);

            logEntityAction(user, tenantId, deviceId, assignedDevice,
                    assignedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_TENANT, null, newTenantId.toString(), newTenant.getName());

            Tenant currentTenant = tenantService.findTenantById(tenantId);
            pushAssignedFromNotification(currentTenant, newTenantId, assignedDevice);

            return assignedDevice;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_TENANT, e, newTenantId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device assignDeviceToEdge(SecurityUser user, TenantId tenantId, DeviceId deviceId, EdgeId edgeId) throws ThingsboardException {
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToEdge(tenantId, deviceId, edgeId));
            Edge edge = edgeService.findEdgeById(tenantId, edgeId);

            logEntityAction(user, tenantId, deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_EDGE, null, deviceId.toString(), edgeId.toString(), edge.getName());

            sendEntityAssignToEdgeNotificationMsg(tenantId, edgeId, savedDevice.getId(), EdgeEventActionType.ASSIGNED_TO_EDGE);

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_EDGE, e, deviceId.toString(), edgeId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Device unassignDeviceFromEdge(SecurityUser user, TenantId tenantId, DeviceId deviceId, EdgeId edgeId) throws ThingsboardException {
        try {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromEdge(tenantId, deviceId, edgeId));
            Edge edge = edgeService.findEdgeById(tenantId, edgeId);

            logEntityAction(user, tenantId, deviceId, device,
                    device.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_EDGE, null, deviceId.toString(), edgeId.toString(), edge.getName());

            sendEntityAssignToEdgeNotificationMsg(tenantId, edgeId, savedDevice.getId(), EdgeEventActionType.UNASSIGNED_FROM_EDGE);

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(user, tenantId, emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.UNASSIGNED_FROM_EDGE, e, deviceId.toString(), edgeId.toString());
            throw handleException(e);
        }
    }

    private void pushAssignedFromNotification(Tenant currentTenant, TenantId newTenantId, Device assignedDevice) {
        String data = entityToStr(assignedDevice);
        if (data != null) {
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_ASSIGNED_FROM_TENANT, assignedDevice.getId(), assignedDevice.getCustomerId(), getMetaDataForAssignedFrom(currentTenant), TbMsgDataType.JSON, data);
            tbClusterService.pushMsgToRuleEngine(newTenantId, assignedDevice.getId(), tbMsg, null);
        }
    }

    private TbMsgMetaData getMetaDataForAssignedFrom(Tenant tenant) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("assignedFromTenantId", tenant.getId().getId().toString());
        metaData.putValue("assignedFromTenantName", tenant.getName());
        return metaData;
    }
}
