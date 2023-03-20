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
package org.thingsboard.server.service.entitiy.asset;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

@Service
@AllArgsConstructor
public class DefaultTbAssetService extends AbstractTbEntityService implements TbAssetService {

    private final AssetService assetService;
    private final TbAssetProfileCache assetProfileCache;

    @Override
    public Asset save(Asset asset, User user) throws Exception {
        ActionType actionType = asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = asset.getTenantId();
        try {
            if (TB_SERVICE_QUEUE.equals(asset.getType())) {
                throw new ThingsboardException("Unable to save asset with type " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } else if (asset.getAssetProfileId() != null) {
                AssetProfile assetProfile = assetProfileCache.get(tenantId, asset.getAssetProfileId());
                if (assetProfile != null && TB_SERVICE_QUEUE.equals(assetProfile.getName())) {
                    throw new ThingsboardException("Unable to save asset with profile " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            Asset savedAsset = checkNotNull(assetService.saveAsset(asset));
            autoCommit(user, savedAsset.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedAsset.getId(), savedAsset,
                    asset.getCustomerId(), actionType, user);
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedAsset.getId(),
                    asset.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), asset, actionType, user, e);
            throw e;
        }
    }

    @Override
    public ListenableFuture<Void> delete(Asset asset, User user) {
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, assetId);
            assetService.deleteAsset(tenantId, assetId);
            notificationEntityService.notifyDeleteEntity(tenantId, assetId, asset, asset.getCustomerId(),
                    ActionType.DELETED, relatedEdgeIds, user, assetId.toString());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetId, ComponentLifecycleEvent.DELETED);
            return removeAlarmsByEntityId(tenantId, assetId);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), ActionType.DELETED, user, e,
                    assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, User user, Boolean unassignAlarms, Boolean removeAlarmComments) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, customerId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, assetId, customerId, savedAsset,
                    actionType, user, assetId.toString(), customerId.toString(), customer.getName());
            if (removeAlarmComments || unassignAlarms) {
                AlarmQuery alarmQuery = new AlarmQuery(assetId, new TimePageLink(Integer.MAX_VALUE),
                        AlarmSearchStatus.ANY, null, null, false);
                PageData<AlarmInfo> alarmInfoPageData = alarmService.findAlarms(tenantId, alarmQuery).get(10, TimeUnit.SECONDS);
                if (!alarmInfoPageData.getData().isEmpty()) {
                    for (AlarmInfo alarmInfo : alarmInfoPageData.getData()) {
                        if (unassignAlarms && alarmInfo.getAssigneeId() != null) {
                            alarmService.unassignAlarm(tenantId, alarmInfo.getId(), System.currentTimeMillis());
                        }
                        if (removeAlarmComments) {
                            PageData<AlarmCommentInfo> alarmComments =
                                    alarmCommentService.findAlarmComments(tenantId, alarmInfo.getId(), new PageLink(Integer.MAX_VALUE));
                            if (!alarmComments.getData().isEmpty()) {
                                alarmComments.getData().forEach(commentInfo -> alarmCommentService.deleteAlarmComment(tenantId, commentInfo.getId()));
                            }
                        }
                    }
                }
            }
            return savedAsset;
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user,
                    e, assetId.toString(), customerId.toString());
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e,
                    assetId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public Asset unassignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(tenantId, assetId));
            CustomerId customerId = customer.getId();
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, assetId, customerId, savedAsset,
                    actionType, user, assetId.toString(), customerId.toString(), customer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset assignAssetToPublicCustomer(TenantId tenantId, AssetId assetId, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, publicCustomer.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, assetId, savedAsset.getCustomerId(), savedAsset,
                    actionType, user, assetId.toString(), publicCustomer.getId().toString(), publicCustomer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToEdge(tenantId, assetId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, assetId, savedAsset.getCustomerId(),
                    edgeId, savedAsset, actionType, user, assetId.toString(), edgeId.toString(), edge.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType,
                    user, e, assetId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    public Asset unassignAssetFromEdge(TenantId tenantId, Asset asset, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        AssetId assetId = asset.getId();
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromEdge(tenantId, assetId, edgeId));

            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, assetId, asset.getCustomerId(),
                    edgeId, asset, actionType, user, assetId.toString(), edgeId.toString(), edge.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType,
                    user, e, assetId.toString(), edgeId.toString());
            throw e;
        }
    }
}
