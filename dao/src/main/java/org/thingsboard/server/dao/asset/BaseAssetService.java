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
package org.thingsboard.server.dao.asset;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.NameConflictPolicy;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("AssetDaoService")
@Slf4j
public class BaseAssetService extends AbstractCachedEntityService<AssetCacheKey, Asset, AssetCacheEvictEvent> implements AssetService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    public static final String INCORRECT_ASSET_PROFILE_ID = "Incorrect assetProfileId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_ASSET_ID = "Incorrect assetId ";

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private DataValidator<Asset> assetValidator;

    @Autowired
    private EntityCountService countService;

    @Autowired
    private JpaExecutorService executor;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AssetCacheEvictEvent event) {
        List<AssetCacheKey> keys = new ArrayList<>(2);
        keys.add(new AssetCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new AssetCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    public AssetInfo findAssetInfoById(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetInfoById [{}]", assetId);
        validateId(assetId, id -> INCORRECT_ASSET_ID + id);
        return assetDao.findAssetInfoById(tenantId, assetId.getId());
    }

    @Override
    public Asset findAssetById(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetById [{}]", assetId);
        validateId(assetId, id -> INCORRECT_ASSET_ID + id);
        return assetDao.findById(tenantId, assetId.getId());
    }

    @Override
    public ListenableFuture<Asset> findAssetByIdAsync(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetByIdAsync [{}]", assetId);
        validateId(assetId, id -> INCORRECT_ASSET_ID + id);
        return assetDao.findByIdAsync(tenantId, assetId.getId());
    }

    @Override
    public Asset findAssetByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findAssetByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return cache.getAndPutInTransaction(new AssetCacheKey(tenantId, name),
                () -> assetDao.findAssetsByTenantIdAndName(tenantId.getId(), name)
                        .orElse(null), true);
    }

    @Override
    public ListenableFuture<Asset> findAssetByTenantIdAndNameAsync(TenantId tenantId, String name) {
        log.trace("Executing findAssetByTenantIdAndNameAsync [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return executor.submit(() -> findAssetByTenantIdAndName(tenantId, name));
    }

    @Override
    public Asset saveAsset(Asset asset) {
        return saveAsset(asset, true);
    }

    @Override
    public Asset saveAsset(Asset asset, NameConflictStrategy nameConflictStrategy) {
        return saveEntity(asset, () -> saveAsset(asset, true, nameConflictStrategy));
    }

    @Override
    public Asset saveAsset(Asset asset, boolean doValidate) {
        return saveEntity(asset, () -> saveAsset(asset, doValidate, NameConflictStrategy.DEFAULT));
    }

    private Asset saveAsset(Asset asset, boolean doValidate, NameConflictStrategy nameConflictStrategy) {
        log.trace("Executing saveAsset [{}]", asset);
        Asset oldAsset = (asset.getId() != null) ? assetDao.findById(asset.getTenantId(), asset.getId().getId()) : null;
        if (nameConflictStrategy.policy() == NameConflictPolicy.UNIQUIFY && (oldAsset == null || !oldAsset.getName().equals(asset.getName()))) {
            uniquifyEntityName(asset, oldAsset, asset::setName, EntityType.ASSET, nameConflictStrategy);
        }
        if (doValidate) {
            assetValidator.validate(asset, Asset::getTenantId);
        }
        AssetCacheEvictEvent evictEvent = new AssetCacheEvictEvent(asset.getTenantId(), asset.getName(), oldAsset != null ? oldAsset.getName() : null);
        Asset savedAsset;
        try {
            AssetProfile assetProfile;
            if (asset.getAssetProfileId() == null) {
                if (!StringUtils.isEmpty(asset.getType())) {
                    assetProfile = this.assetProfileService.findOrCreateAssetProfile(asset.getTenantId(), asset.getType());
                } else {
                    assetProfile = this.assetProfileService.findDefaultAssetProfile(asset.getTenantId());
                }
                asset.setAssetProfileId(new AssetProfileId(assetProfile.getId().getId()));
            } else {
                assetProfile = this.assetProfileService.findAssetProfileById(asset.getTenantId(), asset.getAssetProfileId());
                if (assetProfile == null) {
                    throw new DataValidationException("Asset is referencing non existing asset profile!");
                }
                if (!assetProfile.getTenantId().equals(asset.getTenantId())) {
                    throw new DataValidationException("Asset can`t be referencing to asset profile from different tenant!");
                }
            }
            asset.setType(assetProfile.getName());
            savedAsset = assetDao.saveAndFlush(asset.getTenantId(), asset);
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedAsset.getTenantId()).entityId(savedAsset.getId())
                    .entity(savedAsset).oldEntity(oldAsset).created(asset.getId() == null).build());
            if (asset.getId() == null) {
                countService.publishCountEntityEvictEvent(savedAsset.getTenantId(), EntityType.ASSET);
            }
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t,
                    "asset_name_unq_key", "Asset with such name already exists!",
                    "asset_external_id_unq_key", "Asset with such external id already exists!");
            throw t;
        }
        return savedAsset;
    }

    @Override
    public Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, CustomerId customerId) {
        Asset asset = findAssetById(tenantId, assetId);
        if (customerId.equals(asset.getCustomerId())) {
            return asset;
        }
        asset.setCustomerId(customerId);
        return saveAsset(asset);
    }

    @Override
    public Asset unassignAssetFromCustomer(TenantId tenantId, AssetId assetId) {
        Asset asset = findAssetById(tenantId, assetId);
        if (asset.getCustomerId() == null) {
            return asset;
        }
        asset.setCustomerId(null);
        return saveAsset(asset);
    }

    @Override
    @Transactional
    public void deleteAsset(TenantId tenantId, AssetId assetId) {
        validateId(assetId, id -> INCORRECT_ASSET_ID + id);
        deleteEntity(tenantId, assetId, false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        if (!force && (entityViewService.existsByTenantIdAndEntityId(tenantId, id) || calculatedFieldService.referencedInAnyCalculatedField(tenantId, id))) {
            throw new DataValidationException("Can't delete asset that has entity views or is referenced in calculated fields!");
        }

        Asset asset = assetDao.findById(tenantId, id.getId());
        if (asset == null) {
            return;
        }
        deleteAsset(tenantId, asset);
    }

    private void deleteAsset(TenantId tenantId, Asset asset) {
        log.trace("Executing deleteAsset [{}]", asset.getId());
        assetDao.removeById(tenantId, asset.getUuidId());

        publishEvictEvent(new AssetCacheEvictEvent(asset.getTenantId(), asset.getName(), null));
        countService.publishCountEntityEvictEvent(tenantId, EntityType.ASSET);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(asset.getId()).entity(asset).build());
    }

    @Override
    public PageData<Asset> findAssetsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndAssetProfileId, tenantId [{}], assetProfileId [{}], pageLink [{}]", tenantId, assetProfileId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(assetProfileId, id -> INCORRECT_ASSET_PROFILE_ID + id);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndAssetProfileId(tenantId.getId(), assetProfileId.getId(), pageLink);
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(PageLink pageLink) {
        log.trace("Executing findProfileEntityIdInfos, pageLink [{}]", pageLink);
        validatePageLink(pageLink);
        return assetDao.findProfileEntityIdInfos(pageLink);
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findProfileEntityIdInfosByTenantId, tenantId[{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return assetDao.findProfileEntityIdInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AssetId> findAssetIdsByTenantIdAndAssetProfileId(TenantId tenantId, AssetProfileId assetProfileId, PageLink pageLink) {
        log.trace("Executing findAssetIdsByTenantIdAndAssetProfileId, tenantId [{}], assetProfileId [{}]", tenantId, assetProfileId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(assetProfileId, id -> INCORRECT_ASSET_PROFILE_ID + id);
        validatePageLink(pageLink);
        return assetDao.findAssetIdsByTenantIdAndAssetProfileId(tenantId.getId(), assetProfileId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(TenantId tenantId, List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndIdsAsync, tenantId [{}], assetIds [{}]", tenantId, assetIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(assetIds, ids -> "Incorrect assetIds " + ids);
        return assetDao.findAssetsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(assetIds));
    }

    @Override
    public void deleteAssetsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAssetsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantAssetsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteAssetsByTenantId(tenantId);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(TenantId tenantId, CustomerId customerId, AssetProfileId assetProfileId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId, tenantId [{}], customerId [{}], assetProfileId [{}], pageLink [{}]", tenantId, customerId, assetProfileId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateId(assetProfileId, id -> INCORRECT_ASSET_PROFILE_ID + id);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(tenantId.getId(), customerId.getId(), assetProfileId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], assetIds [{}]", tenantId, customerId, assetIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateIds(assetIds, ids -> "Incorrect assetIds " + ids);
        return assetDao.findAssetsByTenantIdAndCustomerIdAndIdsAsync(tenantId.getId(), customerId.getId(), toUUIDs(assetIds));
    }

    @Override
    public void unassignCustomerAssets(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerAssets, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        customerAssetsUnasigner.removeEntities(tenantId, customerId);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByQuery(TenantId tenantId, AssetSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<Asset>> assets = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Asset>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.ASSET) {
                    futures.add(findAssetByIdAsync(tenantId, new AssetId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());
        assets = Futures.transform(assets, assetList ->
                        assetList == null ?
                                Collections.emptyList() :
                                assetList.stream()
                                        .filter(asset -> query.getAssetTypes().contains(asset.getType()))
                                        .collect(Collectors.toList()),
                MoreExecutors.directExecutor()
        );
        return assets;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findAssetTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findAssetTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return assetDao.findTenantAssetTypesAsync(tenantId.getId());
    }

    @Override
    public Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, EdgeId edgeId) {
        Asset asset = findAssetById(tenantId, assetId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign asset to non-existent edge!");
        }
        if (!edge.getTenantId().getId().equals(asset.getTenantId().getId())) {
            throw new DataValidationException("Can't assign asset to edge from different tenant!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create asset relation. Edge Id: [{}]", assetId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(assetId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).build());
        return asset;
    }

    @Override
    public Asset unassignAssetFromEdge(TenantId tenantId, AssetId assetId, EdgeId edgeId) {
        Asset asset = findAssetById(tenantId, assetId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign asset from non-existent edge!");
        }

        checkAssignedEntityViewsToEdge(tenantId, assetId, edgeId);

        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete asset relation. Edge Id: [{}]", assetId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(assetId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return asset;
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(edgeId, id -> INCORRECT_EDGE_ID + id);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndEdgeIdAndType, tenantId [{}], edgeId [{}], type [{}] pageLink [{}]", tenantId, edgeId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(edgeId, id -> INCORRECT_EDGE_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndEdgeIdAndType(tenantId.getId(), edgeId.getId(), type, pageLink);
    }

    private final PaginatedRemover<TenantId, Asset> tenantAssetsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Asset> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return assetDao.findAssetsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Asset asset) {
            deleteAsset(tenantId, asset);
        }
    };

    private final PaginatedRemover<CustomerId, Asset> customerAssetsUnasigner = new PaginatedRemover<CustomerId, Asset>() {

        @Override
        protected PageData<Asset> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Asset entity) {
            unassignAssetFromCustomer(tenantId, new AssetId(entity.getId().getId()));
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAssetById(tenantId, new AssetId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(findAssetByIdAsync(tenantId, new AssetId(entityId.getId())))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return assetDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
