/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.msg.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntityStateSourcingListener {

    private final TbClusterService tbClusterService;

    @PostConstruct
    public void init() {
        log.info("EntityStateSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent<?> event) {
        log.trace("[{}] SaveEntityEvent called: {}", event.getTenantId(), event);
        TenantId tenantId = event.getTenantId();
        EntityId entityId = event.getEntityId();
        EntityType entityType = entityId.getEntityType();
        boolean isCreated = event.getAdded() != null && event.getAdded();
        ComponentLifecycleEvent lifecycleEvent = isCreated ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED;

        if (isCommonEntityStateUpdated(entityId)) {
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityId, lifecycleEvent);
        } else if (EntityType.TENANT.equals(entityType)) {
            onTenantUpdate((Tenant) event.getEntity(), isCreated);
        } else if (EntityType.TENANT_PROFILE.equals(entityType)) {
            onTenantProfileUpdate((TenantProfile) event.getEntity(), isCreated);
        } else if (EntityType.DEVICE.equals(entityType)) {
            onDeviceUpdate(event.getEntity(), event.getOldEntity());
        } else if (EntityType.DEVICE_PROFILE.equals(entityType)) {
            onDeviceProfileUpdate((DeviceProfile) event.getEntity(), event.getOldEntity(), isCreated);
        } else if (EntityType.EDGE.equals(entityType)) {
            handleEdgeEvent(tenantId, entityId, event.getEntity(), lifecycleEvent);
        } else if (EntityType.TB_RESOURCE.equals(entityType)) {
            tbClusterService.onResourceChange((TbResource) event.getEntity(), null);
        } else if (EntityType.API_USAGE_STATE.equals(entityType) && !event.getAdded()) {
            tbClusterService.onApiStateChange((ApiUsageState) event.getEntity(), null);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        log.trace("[{}] DeleteEntityEvent called: {}", event.getTenantId(), event);
        TenantId tenantId = event.getTenantId();
        EntityId entityId = event.getEntityId();
        EntityType entityType = entityId.getEntityType();

        if (isCommonEntityStateUpdated(entityId) || EntityType.CUSTOMER.equals(entityType)) {
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityId, ComponentLifecycleEvent.DELETED);
        } else if (EntityType.TENANT.equals(event.getEntityId().getEntityType())) {
            onTenantDeleted((Tenant) event.getEntity());
        } else if (EntityType.TENANT_PROFILE.equals(entityType)) {
            tbClusterService.onTenantProfileDelete((TenantProfile) event.getEntity(), null);
        } else if (EntityType.DEVICE.equals(entityType)) {
            tbClusterService.onDeviceDeleted((Device) event.getEntity(), null);
        } else if (EntityType.DEVICE_PROFILE.equals(entityType)) {
            onDeviceProfileDelete(event.getTenantId(), event.getEntityId(), (DeviceProfile) event.getEntity());
        } else if (EntityType.EDGE.equals(entityType)) {
            tbClusterService.broadcastEntityStateChangeEvent(event.getTenantId(), event.getEntityId(), ComponentLifecycleEvent.DELETED);
        } else if (EntityType.TB_RESOURCE.equals(entityType)) {
            tbClusterService.onResourceDeleted((TbResource) event.getEntity(), null);
        }
    }

    private void onDeviceProfileDelete(TenantId tenantId, EntityId entityId, DeviceProfile deviceProfile) {
        tbClusterService.onDeviceProfileDelete(deviceProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityId, ComponentLifecycleEvent.DELETED);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent<?> event) {
        log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);

        if (ActionType.CREDENTIALS_UPDATED.equals(event.getActionType()) && EntityType.DEVICE.equals(event.getEntityId().getEntityType())
                && event.getEntity() instanceof DeviceCredentials) {
            tbClusterService.pushMsgToCore(new DeviceCredentialsUpdateNotificationMsg(
                    event.getTenantId(), (DeviceId) event.getEntityId(), (DeviceCredentials) event.getEntity()), null);
        } else if (ActionType.ASSIGNED_TO_TENANT.equals(event.getActionType()) && event.getEntity() instanceof Device) {
            Tenant tenant = JacksonUtil.fromString(event.getBody(), Tenant.class);
            pushAssignedFromNotification(tenant, event.getTenantId(), (Device) event.getEntity());
        }
    }

    private void onTenantUpdate(Tenant tenant, boolean isCreated) {
        tbClusterService.onTenantChange(tenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenant.getId(), tenant.getId(), isCreated ?
                ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
    }

    private void onTenantProfileUpdate(TenantProfile tenantProfile, boolean isCreated) {
        tbClusterService.onTenantProfileChange(tenantProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID, tenantProfile.getId(),
                isCreated ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
    }

    private boolean isCommonEntityStateUpdated(EntityId entityId) {
        switch (entityId.getEntityType()) {
            case ASSET:
            case ASSET_PROFILE:
            case ENTITY_VIEW:
                return true;
        }
        return false;
    }

    private void onDeviceProfileUpdate(DeviceProfile deviceProfile, Object oldEntity, boolean isCreated) {
        DeviceProfile oldDeviceProfile = null;
        if (!isCreated) {
            oldDeviceProfile = getOldDeviceProfile(oldEntity);
        }
        tbClusterService.onDeviceProfileChange(deviceProfile, oldDeviceProfile, null);
    }

    private DeviceProfile getOldDeviceProfile(Object oldEntity) {
        if (oldEntity instanceof DeviceProfile) {
            return (DeviceProfile) oldEntity;
        }
        return null;
    }

    private void onDeviceUpdate(Object entity, Object oldEntity) {
        Device device = (Device) entity;
        Device oldDevice = null;
        if (oldEntity instanceof Device) {
            oldDevice = (Device) oldEntity;
        }
        tbClusterService.onDeviceUpdated(device, oldDevice);
    }

    private void onTenantDeleted(Tenant tenant) {
        tbClusterService.onTenantDelete(tenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenant.getId(), tenant.getId(), ComponentLifecycleEvent.DELETED);
    }

    private void handleEdgeEvent(TenantId tenantId, EntityId entityId, Object entity, ComponentLifecycleEvent lifecycleEvent) {
        if (entity instanceof Edge) {
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityId, lifecycleEvent);
        } else if (entity instanceof EdgeEvent) {
            tbClusterService.onEdgeEventUpdate(tenantId, (EdgeId) entityId);
        }
    }

    private void pushAssignedFromNotification(Tenant currentTenant, TenantId newTenantId, Device assignedDevice) {
        String data = JacksonUtil.toString(JacksonUtil.valueToTree(assignedDevice));
        if (data != null) {
            TbMsg tbMsg = TbMsg.newMsg(TbMsgType.ENTITY_ASSIGNED_FROM_TENANT, assignedDevice.getId(),
                    assignedDevice.getCustomerId(), getMetaDataForAssignedFrom(currentTenant), TbMsgDataType.JSON, data);
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
