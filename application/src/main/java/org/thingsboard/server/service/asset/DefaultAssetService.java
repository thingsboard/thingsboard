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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.BaseService;
import org.thingsboard.server.service.importing.BulkImportRequest;
import org.thingsboard.server.service.importing.BulkImportResult;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.ASSET_ID;
import static org.thingsboard.server.controller.EdgeController.EDGE_ID;
import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultAssetService extends BaseService implements AssetService {
    private final AssetBulkImportService assetBulkImportService;

    @Override
    public Asset getAssetById(String strAssetId) throws ThingsboardException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            AssetId assetId = new AssetId(toUUID(strAssetId));
            return checkAssetId(assetId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public AssetInfo getAssetInfoById(String strAssetId) throws ThingsboardException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            AssetId assetId = new AssetId(toUUID(strAssetId));
            return checkAssetInfoId(assetId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public Asset saveAsset(Asset asset) throws ThingsboardException {
        try {
            if (TB_SERVICE_QUEUE.equals(asset.getType())) {
                throw new ThingsboardException("Unable to save asset with type " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }

            asset.setTenantId(getCurrentUser().getTenantId());

            checkEntity(asset.getId(), asset, Resource.ASSET);

            Asset savedAsset = checkNotNull(assetService.saveAsset(asset));

            onAssetCreatedOrUpdated(savedAsset, asset.getId() != null, getCurrentUser());

            return savedAsset;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ASSET), asset,
                    null, asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }

    }

    @Override
    public void deleteAsset(String strAssetId) throws ThingsboardException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            AssetId assetId = new AssetId(toUUID(strAssetId));
            Asset asset = checkAssetId(assetId, Operation.DELETE);

            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), assetId);

            assetService.deleteAsset(getTenantId(), assetId);

            logEntityAction(assetId, asset,
                    asset.getCustomerId(),
                    ActionType.DELETED, null, strAssetId);

            sendDeleteNotificationMsg(getTenantId(), assetId, relatedEdgeIds);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ASSET),
                    null,
                    null,
                    ActionType.DELETED, e, strAssetId);
            throw handleException(e);
        }
    }

    @Override
    public Asset assignAssetToCustomer(String strCustomerId, String strAssetId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(ASSET_ID, strAssetId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId, Operation.READ);

            AssetId assetId = new AssetId(toUUID(strAssetId));
            checkAssetId(assetId, Operation.ASSIGN_TO_CUSTOMER);

            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(getTenantId(), assetId, customerId));

            logEntityAction(assetId, savedAsset,
                    savedAsset.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strAssetId, strCustomerId, customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedAsset.getTenantId(), savedAsset.getId(),
                    customerId, EdgeEventActionType.ASSIGNED_TO_CUSTOMER);

            return savedAsset;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ASSET), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strAssetId, strCustomerId);

            throw handleException(e);
        }
    }

    @Override
    public Asset unassignAssetFromCustomer(String strAssetId) throws ThingsboardException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            AssetId assetId = new AssetId(toUUID(strAssetId));
            Asset asset = checkAssetId(assetId, Operation.UNASSIGN_FROM_CUSTOMER);
            if (asset.getCustomerId() == null || asset.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Asset isn't assigned to any customer!");
            }

            Customer customer = checkCustomerId(asset.getCustomerId(), Operation.READ);

            Asset savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(getTenantId(), assetId));

            logEntityAction(assetId, asset,
                    asset.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strAssetId, customer.getId().toString(), customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedAsset.getTenantId(), savedAsset.getId(),
                    customer.getId(), EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER);

            return savedAsset;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ASSET), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strAssetId);

            throw handleException(e);
        }
    }

    @Override
    public Asset assignAssetToPublicCustomer(String strAssetId) throws ThingsboardException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            AssetId assetId = new AssetId(toUUID(strAssetId));
            Asset asset = checkAssetId(assetId, Operation.ASSIGN_TO_CUSTOMER);
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(asset.getTenantId());
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(getTenantId(), assetId, publicCustomer.getId()));

            logEntityAction(assetId, savedAsset,
                    savedAsset.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strAssetId, publicCustomer.getId().toString(), publicCustomer.getName());

            return savedAsset;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ASSET), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strAssetId);

            throw handleException(e);
        }
    }

    @Override
    public PageData<Asset> getTenantAssets(int pageSize, int page, String type, String textSearch, String sortProperty,
                                           String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(assetService.findAssetsByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(assetService.findAssetsByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public PageData<AssetInfo> getTenantAssetInfos(int pageSize, int page, String type, String textSearch, String sortProperty,
                                                   String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(assetService.findAssetInfosByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(assetService.findAssetInfosByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public Asset getTenantAsset(String assetName) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(assetService.findAssetByTenantIdAndName(tenantId, assetName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public PageData<Asset> getCustomerAssets(String strCustomerId, int pageSize, int page, String type, String textSearch,
                                             String sortProperty, String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public PageData<AssetInfo> getCustomerAssetInfos(String strCustomerId, int pageSize, int page, String type,
                                                     String textSearch, String sortProperty, String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public List<Asset> getAssetsByIds(String[] strAssetIds) throws ThingsboardException {
        checkArrayParameter("assetIds", strAssetIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();
            List<AssetId> assetIds = new ArrayList<>();
            for (String strAssetId : strAssetIds) {
                assetIds.add(new AssetId(toUUID(strAssetId)));
            }
            ListenableFuture<List<Asset>> assets;
            if (customerId == null || customerId.isNullUid()) {
                assets = assetService.findAssetsByTenantIdAndIdsAsync(tenantId, assetIds);
            } else {
                assets = assetService.findAssetsByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, assetIds);
            }
            return checkNotNull(assets.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public List<Asset> findByQuery(AssetSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getAssetTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<Asset> assets = checkNotNull(assetService.findAssetsByQuery(getTenantId(), query).get());
            assets = assets.stream().filter(asset -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ, asset.getId(), asset);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return assets;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public List<EntitySubtype> getAssetTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> assetTypes = assetService.findAssetTypesByTenantId(tenantId);
            return checkNotNull(assetTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public Asset assignAssetToEdge(String strEdgeId, String strAssetId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(ASSET_ID, strAssetId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.READ);

            AssetId assetId = new AssetId(toUUID(strAssetId));
            checkAssetId(assetId, Operation.READ);

            Asset savedAsset = checkNotNull(assetService.assignAssetToEdge(getTenantId(), assetId, edgeId));

            logEntityAction(assetId, savedAsset,
                    savedAsset.getCustomerId(),
                    ActionType.ASSIGNED_TO_EDGE, null, strAssetId, strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, savedAsset.getId(), EdgeEventActionType.ASSIGNED_TO_EDGE);

            return savedAsset;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ASSET), null,
                    null,
                    ActionType.ASSIGNED_TO_EDGE, e, strAssetId, strEdgeId);

            throw handleException(e);
        }
    }

    @Override
    public Asset unassignAssetFromEdge(String strEdgeId, String strAssetId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(ASSET_ID, strAssetId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.READ);

            AssetId assetId = new AssetId(toUUID(strAssetId));
            Asset asset = checkAssetId(assetId, Operation.READ);

            Asset savedAsset = checkNotNull(assetService.unassignAssetFromEdge(getTenantId(), assetId, edgeId));

            logEntityAction(assetId, asset,
                    asset.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_EDGE, null, strAssetId, strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, savedAsset.getId(), EdgeEventActionType.UNASSIGNED_FROM_EDGE);

            return savedAsset;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ASSET), null,
                    null,
                    ActionType.UNASSIGNED_FROM_EDGE, e, strAssetId, strEdgeId);

            throw handleException(e);
        }
    }

    @Override
    public PageData<Asset> getEdgeAssets(String strEdgeId, int pageSize, int page, String type, String textSearch,
                                         String sortProperty, String sortOrder, Long startTime, Long endTime) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            checkEdgeId(edgeId, Operation.READ);
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            PageData<Asset> nonFilteredResult;
            if (type != null && type.trim().length() > 0) {
                nonFilteredResult = assetService.findAssetsByTenantIdAndEdgeIdAndType(tenantId, edgeId, type, pageLink);
            } else {
                nonFilteredResult = assetService.findAssetsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            }
            List<Asset> filteredAssets = nonFilteredResult.getData().stream().filter(asset -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ, asset.getId(), asset);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            PageData<Asset> filteredResult = new PageData<>(filteredAssets,
                    nonFilteredResult.getTotalPages(),
                    nonFilteredResult.getTotalElements(),
                    nonFilteredResult.hasNext());
            return checkNotNull(filteredResult);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public BulkImportResult<Asset> processAssetsBulkImport(BulkImportRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        return assetBulkImportService.processBulkImport(request, user, importedAssetInfo -> {
            onAssetCreatedOrUpdated(importedAssetInfo.getEntity(), importedAssetInfo.isUpdated(), user);
        });
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
