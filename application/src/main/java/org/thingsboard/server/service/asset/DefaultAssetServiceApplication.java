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
package org.thingsboard.server.service.asset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.BaseServiceApplication;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;
import static org.thingsboard.server.utils.CheckUtils.checkNotNull;
import static org.thingsboard.server.utils.CheckUtils.getTenantId;
import static org.thingsboard.server.utils.CheckUtils.toUUID;

@Slf4j
@TbCoreComponent
@Service
public class DefaultAssetServiceApplication extends BaseServiceApplication implements AssetServiceApplication {


    @Override
    public Asset saveAsset(Asset asset, SecurityUser currentUser) throws ThingsboardException {
        try {
            Asset savedAsset = saveAsset(asset, currentUser.getTenantId()) ;
            onAssetCreatedOrUpdated(savedAsset, asset.getId() != null, getCurrentUser());
            return savedAsset;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ASSET), asset,
                    null, asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @Override
    public Asset saveAsset(Asset asset, TenantId tenantId) throws ThingsboardException {
        try {
            if (TB_SERVICE_QUEUE.equals(asset.getType())) {
                throw new ThingsboardException("Unable to save asset with type " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }

            asset.setTenantId(tenantId);
            checkEntity(asset.getId(), asset, Resource.ASSET);
            return checkNotNull(assetService.saveAsset(asset));
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ASSET), asset,
                    null, asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @Override
    public void deleteAsset(AssetId assetId) throws ThingsboardException {
        try {
            Asset asset = checkAssetId(assetId, Operation.DELETE);

            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), assetId);

            assetService.deleteAsset(getTenantId(), assetId);

            logEntityAction(assetId, asset,
                    asset.getCustomerId(),
                    ActionType.DELETED, null, assetId.getId().toString());

            sendDeleteNotificationMsg(getTenantId(), assetId, relatedEdgeIds);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ASSET),
                    null,
                    null,
                    ActionType.DELETED, e, assetId.getId().toString());
            throw handleException(e);
        }
    }

    @Override
    public Asset assignAssetToCustomer(CustomerId customerId, AssetId assetId) throws ThingsboardException {
        try {
            Customer customer = checkCustomerId(customerId, Operation.READ);
            checkAssetId(assetId, Operation.ASSIGN_TO_CUSTOMER);

            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(getTenantId(), assetId, customerId));

            logEntityAction(assetId, savedAsset,
                    savedAsset.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, assetId.getId().toString(), customerId.getId().toString(), customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedAsset.getTenantId(), savedAsset.getId(),
                    customerId, EdgeEventActionType.ASSIGNED_TO_CUSTOMER);

            return savedAsset;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ASSET), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, assetId.getId().toString(), customerId.getId().toString());

            throw handleException(e);
        }
    }

    @Override
    public Asset unassignAssetFromCustomer(AssetId assetId) throws ThingsboardException {
        try {
            Asset asset = checkAssetId(assetId, Operation.UNASSIGN_FROM_CUSTOMER);
            if (asset.getCustomerId() == null || asset.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Asset isn't assigned to any customer!");
            }

            Customer customer = checkCustomerId(asset.getCustomerId(), Operation.READ);

            Asset savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(getTenantId(), assetId));

            logEntityAction(assetId, asset,
                    asset.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, assetId.getId().toString(), customer.getId().toString(), customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedAsset.getTenantId(), savedAsset.getId(),
                    customer.getId(), EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER);

            return savedAsset;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ASSET), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, assetId.getId().toString());

            throw handleException(e);
        }
    }

    @Override
    public Asset assignAssetToPublicCustomer(AssetId assetId) throws ThingsboardException {
        try {
            Asset asset = checkAssetId(assetId, Operation.ASSIGN_TO_CUSTOMER);
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(asset.getTenantId());
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(getTenantId(), assetId, publicCustomer.getId()));

            logEntityAction(assetId, savedAsset,
                    savedAsset.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, assetId.getId().toString(), publicCustomer.getId().toString(), publicCustomer.getName());

            return savedAsset;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ASSET), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, assetId.getId().toString());

            throw handleException(e);
        }
    }


    private void onAssetCreatedOrUpdated(Asset asset, boolean updated, SecurityUser user) {
        try {
            logEntityAction(user, asset.getId(), asset,
                    asset.getCustomerId(),
                    updated ? ActionType.UPDATED : ActionType.ADDED, null);
        } catch (ThingsboardException e) {
            log.error("Failed to log entity action", e);
        }

        if (updated) {
            sendEntityNotificationMsg(asset.getTenantId(), asset.getId(), EdgeEventActionType.UPDATED);
        }
    }

}
