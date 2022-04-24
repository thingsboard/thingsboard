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
package org.thingsboard.server.service.entity;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.dao.device.claim.ReclaimResult;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@TbCoreComponent
@RequiredArgsConstructor
@Service
@Slf4j
public class DefaultEntityDeleteService extends BaseController implements EntityDeleteService {

    private final GatewayNotificationsService gatewayNotificationsService;
    private final AlarmSubscriptionService alarmService;

    @Override
    public void deleteEntity(TenantId tenantId, EntityId entityId) throws ThingsboardException {
        try {
            List<AlarmId> alarmIds = alarmService.findAlarms(tenantId, new AlarmQuery(entityId, new TimePageLink(Integer.MAX_VALUE), null, null, false))
                    .get().getData().stream().map(AlarmInfo::getId).collect(Collectors.toList());
            alarmIds.forEach(alarmId -> {
                alarmService.deleteAlarm(tenantId, alarmId);
            });
        } catch (Exception e) {
            log.error("[{}] Failed to delete alarms for entity", entityId.getId(), e);
        }
        switch (entityId.getEntityType()) {
            case RULE_CHAIN:
                deleteRuleChain(new RuleChainId(entityId.getId()));
                break;
            case DEVICE_PROFILE:
                deleteProfile(new DeviceProfileId(entityId.getId()));
                break;
            case DASHBOARD:
                deleteDashboard(new DashboardId(entityId.getId()));
                break;
            case USER:
                deleteUser(new UserId(entityId.getId()));
                break;
            case ASSET:
                deleteAsset(new AssetId(entityId.getId()));
                break;
            case DEVICE:
                deleteDevice(new DeviceId(entityId.getId()));
                break;
            case CUSTOMER:
                deleteCustomer(new CustomerId(entityId.getId()));
                break;
            case EDGE:
                deleteEdge(new EdgeId(entityId.getId()));
                break;
            case ENTITY_VIEW:
                deleteEntityView(new EntityViewId(entityId.getId()));
                break;
            case OTA_PACKAGE:
                deleteOtaPackage(new OtaPackageId(entityId.getId()));
                break;
            case TB_RESOURCE:
                deleteTbResource(new TbResourceId(entityId.getId()));
                break;
            case TENANT:
                deleteTenant(new TenantId(entityId.getId()));
                break;
            case TENANT_PROFILE:
                deleteTenantProfile(new TenantProfileId((entityId.getId())));
                break;
            case WIDGETS_BUNDLE:
                deleteWidgetsBundle(new WidgetsBundleId((entityId.getId())));
                break;
            case WIDGET_TYPE:
                deleteWidgetType(new WidgetTypeId((entityId.getId())));
                break;
        }
    }

    @Override
    public Boolean deleteAlarm(AlarmId alarmId) throws ThingsboardException {
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);

        List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), alarm.getOriginator());

        logEntityAction(alarm.getOriginator(), alarm,
                getCurrentUser().getCustomerId(),
                ActionType.ALARM_DELETE, null);

        sendAlarmDeleteNotificationMsg(getTenantId(), alarmId, relatedEdgeIds, alarm);

        return alarmService.deleteAlarm(getTenantId(), alarmId);
    }

    @Override
    public DeferredResult<ResponseEntity> deleteReClaimDevice(String deviceName) throws ThingsboardException {
        final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();

        Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        accessControlService.checkPermission(user, Resource.DEVICE, Operation.CLAIM_DEVICES,
                device.getId(), device);

        ListenableFuture<ReclaimResult> result = claimDevicesService.reClaimDevice(tenantId, device);
        Futures.addCallback(result, new FutureCallback<>() {
            @Override
            public void onSuccess(ReclaimResult reclaimResult) {
                deferredResult.setResult(new ResponseEntity(HttpStatus.OK));

                Customer unassignedCustomer = reclaimResult.getUnassignedCustomer();
                if (unassignedCustomer != null) {
                    try {
                        logEntityAction(user, device.getId(), device, device.getCustomerId(), ActionType.UNASSIGNED_FROM_CUSTOMER, null,
                                device.getId().toString(), unassignedCustomer.getId().toString(), unassignedCustomer.getName());
                    } catch (ThingsboardException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        }, MoreExecutors.directExecutor());
        return deferredResult;

    }

    @Override
    public void deleteRelation(EntityId fromId, EntityId toId, String strRelationType, String strRelationTypeGroup,
                               EntityRelation relation, RelationTypeGroup relationTypeGroup) throws ThingsboardException {
        Boolean found = relationService.deleteRelation(getTenantId(), fromId, toId, strRelationType, relationTypeGroup);
        if (!found) {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        logEntityAction(relation.getFrom(), null, getCurrentUser().getCustomerId(),
                ActionType.RELATION_DELETED, null, relation);
        logEntityAction(relation.getTo(), null, getCurrentUser().getCustomerId(),
                ActionType.RELATION_DELETED, null, relation);

        sendRelationNotificationMsg(getTenantId(), relation, EdgeEventActionType.RELATION_DELETED);
    }

    @Override
    public void deleteRelations(EntityId entityId) throws ThingsboardException {
        relationService.deleteEntityRelations(getTenantId(), entityId);
        logEntityAction(entityId, null, getCurrentUser().getCustomerId(), ActionType.RELATIONS_DELETED, null);
    }

    @Override
    public RuleChain deleteUnsetAutoAssignToEdgeRuleChain(RuleChainId ruleChainId) throws ThingsboardException {
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
        ruleChainService.unsetAutoAssignToEdgeRuleChain(getTenantId(), ruleChainId);
        return ruleChain;
    }

    @Override
    public RuleChain deleteUnassignRuleChain(String strRuleChainId, String strEdgeId) throws ThingsboardException {
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.WRITE);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.READ);

        RuleChain savedRuleChain = checkNotNull(ruleChainService.unassignRuleChainFromEdge(getCurrentUser().getTenantId(), ruleChainId, edgeId, false));

        logEntityAction(ruleChainId, ruleChain,
                null,
                ActionType.UNASSIGNED_FROM_EDGE, null, strRuleChainId, strEdgeId, edge.getName());

        sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, savedRuleChain.getId(), EdgeEventActionType.UNASSIGNED_FROM_EDGE);

        return savedRuleChain;
    }

    private void deleteRuleChain(RuleChainId ruleChainId) throws ThingsboardException {
        String strRuleChainId = ruleChainId.getId().toString();
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.DELETE);

        List<RuleNode> referencingRuleNodes = ruleChainService.getReferencingRuleChainNodes(getTenantId(), ruleChainId);

        Set<RuleChainId> referencingRuleChainIds = referencingRuleNodes.stream().map(RuleNode::getRuleChainId).collect(Collectors.toSet());

        List<EdgeId> relatedEdgeIds = null;
        if (RuleChainType.EDGE.equals(ruleChain.getType())) {
            relatedEdgeIds = findRelatedEdgeIds(getTenantId(), ruleChainId);
        }

        ruleChainService.deleteRuleChainById(getTenantId(), ruleChainId);

        referencingRuleChainIds.remove(ruleChain.getId());

        if (RuleChainType.CORE.equals(ruleChain.getType())) {
            referencingRuleChainIds.forEach(referencingRuleChainId ->
                    tbClusterService.broadcastEntityStateChangeEvent(ruleChain.getTenantId(), referencingRuleChainId, ComponentLifecycleEvent.UPDATED));

            tbClusterService.broadcastEntityStateChangeEvent(ruleChain.getTenantId(), ruleChain.getId(), ComponentLifecycleEvent.DELETED);
        }

        logEntityAction(ruleChainId, ruleChain,
                null,
                ActionType.DELETED, null, strRuleChainId);

        if (RuleChainType.EDGE.equals(ruleChain.getType())) {
            sendDeleteNotificationMsg(ruleChain.getTenantId(), ruleChain.getId(), relatedEdgeIds);
        }
    }

    private void deleteProfile(DeviceProfileId deviceProfileId) throws ThingsboardException {
        String strDeviceProfileId = deviceProfileId.getId().toString();
        DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.DELETE);
        deviceProfileService.deleteDeviceProfile(getTenantId(), deviceProfileId);

        tbClusterService.onDeviceProfileDelete(deviceProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(getTenantId(), deviceProfile.getId(), ComponentLifecycleEvent.DELETED);

        logEntityAction(deviceProfileId, deviceProfile,
                null,
                ActionType.DELETED, null, strDeviceProfileId);

        sendEntityNotificationMsg(getTenantId(), deviceProfile.getId(), EdgeEventActionType.DELETED);

    }

    private void deleteDashboard(DashboardId dashboardId) throws ThingsboardException {
        String strDashboardId = dashboardId.getId().toString();
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.DELETE);

        List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), dashboardId);

        dashboardService.deleteDashboard(getCurrentUser().getTenantId(), dashboardId);

        logEntityAction(dashboardId, dashboard,
                null,
                ActionType.DELETED, null, strDashboardId);

        sendDeleteNotificationMsg(getTenantId(), dashboardId, relatedEdgeIds);
    }

    private void deleteUser(UserId userId) throws ThingsboardException {
        String strUserId = userId.getId().toString();
        User user = checkUserId(userId, Operation.DELETE);

        if (user.getAuthority() == Authority.SYS_ADMIN && getCurrentUser().getId().equals(userId)) {
            throw new ThingsboardException("Sysadmin is not allowed to delete himself", ThingsboardErrorCode.PERMISSION_DENIED);
        }

        List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), userId);

        userService.deleteUser(getCurrentUser().getTenantId(), userId);

        logEntityAction(userId, user,
                user.getCustomerId(),
                ActionType.DELETED, null, strUserId);

        sendDeleteNotificationMsg(getTenantId(), userId, relatedEdgeIds);
    }

    private void deleteAsset(AssetId assetId) throws ThingsboardException {
        String strAssetId = assetId.getId().toString();
        Asset asset = checkAssetId(assetId, Operation.DELETE);

        List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), assetId);

        assetService.deleteAsset(getTenantId(), assetId);

        logEntityAction(assetId, asset,
                asset.getCustomerId(),
                ActionType.DELETED, null, strAssetId);

        sendDeleteNotificationMsg(getTenantId(), assetId, relatedEdgeIds);
    }


    private void deleteDevice(DeviceId deviceId) throws ThingsboardException {
        String strDeviceId = deviceId.getId().toString();
        Device device = checkDeviceId(deviceId, Operation.DELETE);

        List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), deviceId);

        deviceService.deleteDevice(getCurrentUser().getTenantId(), deviceId);

        gatewayNotificationsService.onDeviceDeleted(device);
        tbClusterService.onDeviceDeleted(device, null);

        logEntityAction(deviceId, device,
                device.getCustomerId(),
                ActionType.DELETED, null, strDeviceId);

        sendDeleteNotificationMsg(getTenantId(), deviceId, relatedEdgeIds);
    }

    private void deleteCustomer(CustomerId customerId) throws ThingsboardException {
        String strCustomerId = customerId.getId().toString();
        Customer customer = checkCustomerId(customerId, Operation.DELETE);

        List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), customerId);

        customerService.deleteCustomer(getTenantId(), customerId);

        logEntityAction(customerId, customer,
                customer.getId(),
                ActionType.DELETED, null, strCustomerId);

        sendDeleteNotificationMsg(getTenantId(), customerId, relatedEdgeIds);
        tbClusterService.broadcastEntityStateChangeEvent(getTenantId(), customerId, ComponentLifecycleEvent.DELETED);
    }

    private void deleteEdge(EdgeId edgeId) throws ThingsboardException {
        String strEdgeId = edgeId.getId().toString();
        Edge edge = checkEdgeId(edgeId, Operation.DELETE);
        edgeService.deleteEdge(getTenantId(), edgeId);

        tbClusterService.broadcastEntityStateChangeEvent(getTenantId(), edgeId,
                ComponentLifecycleEvent.DELETED);

        logEntityAction(edgeId, edge,
                null,
                ActionType.DELETED, null, strEdgeId);
    }

    private void deleteEntityView(EntityViewId entityViewId) throws ThingsboardException {
        String strEntityViewId = entityViewId.getId().toString();
        EntityView entityView = checkEntityViewId(entityViewId, Operation.DELETE);

        List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), entityViewId);

        entityViewService.deleteEntityView(getTenantId(), entityViewId);
        logEntityAction(entityViewId, entityView, entityView.getCustomerId(),
                ActionType.DELETED, null, strEntityViewId);

        sendDeleteNotificationMsg(getTenantId(), entityViewId, relatedEdgeIds);
    }

    private void deleteOtaPackage(OtaPackageId otaPackageId) throws ThingsboardException {
        String strOtaPackageId = otaPackageId.getId().toString();
        OtaPackageInfo info = checkOtaPackageInfoId(otaPackageId, Operation.DELETE);
        otaPackageService.deleteOtaPackage(getTenantId(), otaPackageId);
        logEntityAction(otaPackageId, info, null, ActionType.DELETED, null, strOtaPackageId);
    }

    private void deleteTbResource(TbResourceId resourceId) throws ThingsboardException {
        String strResourceId = resourceId.getId().toString();
        TbResource tbResource = checkResourceId(resourceId, Operation.DELETE);
        resourceService.deleteResource(getTenantId(), resourceId);
        tbClusterService.onResourceDeleted(tbResource, null);
        logEntityAction(resourceId, tbResource, null, ActionType.DELETED, null, strResourceId);
    }

    private void deleteTenant(TenantId tenantId) throws ThingsboardException {
        Tenant tenant = checkTenantId(tenantId, Operation.DELETE);
        tenantService.deleteTenant(tenantId);
        tenantProfileCache.evict(tenantId);
        tbClusterService.onTenantDelete(tenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, tenantId, ComponentLifecycleEvent.DELETED);
    }

    private void deleteTenantProfile(TenantProfileId tenantProfileId) throws ThingsboardException {
        TenantProfile profile = checkTenantProfileId(tenantProfileId, Operation.DELETE);
        tenantProfileService.deleteTenantProfile(getTenantId(), tenantProfileId);
        tbClusterService.onTenantProfileDelete(profile, null);
    }

    private void deleteWidgetsBundle(WidgetsBundleId widgetsBundleId) throws ThingsboardException {
        checkWidgetsBundleId(widgetsBundleId, Operation.DELETE);
        widgetsBundleService.deleteWidgetsBundle(getTenantId(), widgetsBundleId);
        sendEntityNotificationMsg(getTenantId(), widgetsBundleId, EdgeEventActionType.DELETED);
    }

    private void deleteWidgetType(WidgetTypeId widgetTypeId) throws ThingsboardException {
        checkWidgetTypeId(widgetTypeId, Operation.DELETE);
        widgetTypeService.deleteWidgetType(getCurrentUser().getTenantId(), widgetTypeId);
        sendEntityNotificationMsg(getTenantId(), widgetTypeId, EdgeEventActionType.DELETED);
    }
}