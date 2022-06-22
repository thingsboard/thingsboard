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
package org.thingsboard.server.service.entitiy.asset;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

import static org.thingsboard.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbAssetService extends AbstractTbEntityService implements TbAssetService {

    @Override
    public Asset save(Asset asset, SecurityUser user) throws ThingsboardException {
        ActionType actionType = asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = asset.getTenantId();
        try {
            Asset savedAsset = checkNotNull(assetService.saveAsset(asset));
            vcService.autoCommit(user, savedAsset.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedAsset.getId(), savedAsset, savedAsset.getCustomerId(), actionType, user);
            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.ASSET), asset, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public ListenableFuture<Void> delete(Asset asset, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(tenantId, assetId);
            assetService.deleteAsset(tenantId, assetId);
            notificationEntityService.notifyDeleteEntity(tenantId, assetId, asset, asset.getCustomerId(), ActionType.DELETED,
                    relatedEdgeIds, user, assetId.toString());

            return removeAlarmsByEntityId(tenantId, assetId);
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.ASSET), null, null,
                    ActionType.DELETED, user, e, assetId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, customerId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, assetId, customerId, savedAsset,
                    actionType, edgeTypeByActionType(actionType), user, true, customerId.toString(), customer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.ASSET), null, null,
                    actionType, user, e, assetId.toString(), customerId.toString());

            throw handleException(e);
        }
    }

    @Override
    public Asset unassignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(tenantId, assetId));
            CustomerId customerId = customer.getId();

            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, assetId, customerId, savedAsset,
                    actionType, edgeTypeByActionType(actionType), user,
                    true, customerId.toString(), customer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.ASSET), null, null,
                    actionType, user, e, assetId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Asset assignAssetToPublicCustomer(TenantId tenantId, AssetId assetId, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, publicCustomer.getId()));

            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, assetId, savedAsset.getCustomerId(), savedAsset,
                    actionType, null, user, false, actionType.toString(),
                    publicCustomer.getId().toString(), publicCustomer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.ASSET), null, null,
                    actionType, user, e, assetId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, Edge edge, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToEdge(tenantId, assetId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, assetId, savedAsset.getCustomerId(),
                    edgeId, savedAsset, actionType, user, assetId.toString(), edgeId.toString(), edge.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.ASSET), null, null,
                    actionType, user, e, assetId.toString(), edgeId.toString());
            throw handleException(e);
        }
    }

    @Override
    public Asset unassignAssetFromEdge(TenantId tenantId, Asset asset, Edge edge, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        AssetId assetId = asset.getId();
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromEdge(tenantId, assetId, edgeId));

            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, assetId, asset.getCustomerId(),
                    edgeId, asset, actionType, user, assetId.toString(), edgeId.toString(), edge.getName());
            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.ASSET), null, null,
                    actionType, user, e, assetId.toString(), edgeId.toString());
            throw handleException(e);
        }
    }
}
