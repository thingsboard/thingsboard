/**
 * Copyright © 2016-2021 The Thingsboard Authors
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


import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.ASSET_CACHE;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class BaseAssetService extends AbstractEntityService implements AssetService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_ASSET_ID = "Incorrect assetId ";
    public static final String TB_SERVICE_QUEUE = "TbServiceQueue";

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    public AssetInfo findAssetInfoById(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetInfoById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findAssetInfoById(tenantId, assetId.getId());
    }

    @Override
    public Asset findAssetById(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findById(tenantId, assetId.getId());
    }

    @Override
    public ListenableFuture<Asset> findAssetByIdAsync(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findByIdAsync(tenantId, assetId.getId());
    }

    @Cacheable(cacheNames = ASSET_CACHE, key = "{#tenantId, #name}")
    @Override
    public Asset findAssetByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findAssetByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return assetDao.findAssetsByTenantIdAndName(tenantId.getId(), name)
                .orElse(null);
    }

    @CacheEvict(cacheNames = ASSET_CACHE, key = "{#asset.tenantId, #asset.name}")
    @Override
    public Asset saveAsset(Asset asset) {
        log.trace("Executing saveAsset [{}]", asset);
        assetValidator.validate(asset, Asset::getTenantId);
        Asset savedAsset;
        try {
            savedAsset = assetDao.save(asset.getTenantId(), asset);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("asset_name_unq_key")) {
                throw new DataValidationException("Asset with such name already exists!");
            } else {
                throw t;
            }
        }
        return savedAsset;
    }

    @Override
    public Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, CustomerId customerId) {
        Asset asset = findAssetById(tenantId, assetId);
        asset.setCustomerId(customerId);
        return saveAsset(asset);
    }

    @Override
    public Asset unassignAssetFromCustomer(TenantId tenantId, AssetId assetId) {
        Asset asset = findAssetById(tenantId, assetId);
        asset.setCustomerId(null);
        return saveAsset(asset);
    }

    @Override
    public void deleteAsset(TenantId tenantId, AssetId assetId) {
        log.trace("Executing deleteAsset [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        deleteEntityRelations(tenantId, assetId);

        Asset asset = assetDao.findById(tenantId, assetId.getId());
        try {
            List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(asset.getTenantId(), assetId).get();
            if (entityViews != null && !entityViews.isEmpty()) {
                throw new DataValidationException("Can't delete asset that has entity views!");
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Exception while finding entity views for assetId [{}]", assetId, e);
            throw new RuntimeException("Exception while finding entity views for assetId [" + assetId + "]", e);
        }

        List<Object> list = new ArrayList<>();
        list.add(asset.getTenantId());
        list.add(asset.getName());
        Cache cache = cacheManager.getCache(ASSET_CACHE);
        cache.evict(list);

        assetDao.removeById(tenantId, assetId.getId());
    }

    @Override
    public PageData<Asset> findAssetsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(TenantId tenantId, List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndIdsAsync, tenantId [{}], assetIds [{}]", tenantId, assetIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(assetIds, "Incorrect assetIds " + assetIds);
        return assetDao.findAssetsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(assetIds));
    }

    @Override
    public void deleteAssetsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAssetsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAssetsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], assetIds [{}]", tenantId, customerId, assetIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateIds(assetIds, "Incorrect assetIds " + assetIds);
        return assetDao.findAssetsByTenantIdAndCustomerIdAndIdsAsync(tenantId.getId(), customerId.getId(), toUUIDs(assetIds));
    }

    @Override
    public void unassignCustomerAssets(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerAssets, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
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
                assetList == null ? Collections.emptyList() : assetList.stream().filter(asset -> query.getAssetTypes().contains(asset.getType())).collect(Collectors.toList()), MoreExecutors.directExecutor()
        );
        return assets;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findAssetTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findAssetTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantAssetTypes = assetDao.findTenantAssetTypesAsync(tenantId.getId());
        return Futures.transform(tenantAssetTypes,
                assetTypes -> {
                    assetTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return assetTypes;
                }, MoreExecutors.directExecutor());
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
        return asset;
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, TimePageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, TimePageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndEdgeIdAndType, tenantId [{}], edgeId [{}], type [{}] pageLink [{}]", tenantId, edgeId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndEdgeIdAndType(tenantId.getId(), edgeId.getId(), type, pageLink);
    }

    private DataValidator<Asset> assetValidator =
            new DataValidator<Asset>() {

                @Override
                protected void validateCreate(TenantId tenantId, Asset asset) {
                    DefaultTenantProfileConfiguration profileConfiguration =
                            (DefaultTenantProfileConfiguration)tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
                    if (!TB_SERVICE_QUEUE.equals(asset.getType())) {
                        long maxAssets = profileConfiguration.getMaxAssets();
                        validateNumberOfEntitiesPerTenant(tenantId, assetDao, maxAssets, EntityType.ASSET);
                    }
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Asset asset) {
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Asset asset) {
                    if (StringUtils.isEmpty(asset.getType())) {
                        throw new DataValidationException("Asset type should be specified!");
                    }
                    if (StringUtils.isEmpty(asset.getName())) {
                        throw new DataValidationException("Asset name should be specified!");
                    }
                    if (asset.getTenantId() == null) {
                        throw new DataValidationException("Asset should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, asset.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Asset is referencing to non-existent tenant!");
                        }
                    }
                    if (asset.getCustomerId() == null) {
                        asset.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!asset.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(tenantId, asset.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign asset to non-existent customer!");
                        }
                        if (!customer.getTenantId().equals(asset.getTenantId())) {
                            throw new DataValidationException("Can't assign asset to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Asset> tenantAssetsRemover =
            new PaginatedRemover<TenantId, Asset>() {

                @Override
                protected PageData<Asset> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return assetDao.findAssetsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Asset entity) {
                    deleteAsset(tenantId, new AssetId(entity.getId().getId()));
                }
            };

    private PaginatedRemover<CustomerId, Asset> customerAssetsUnasigner = new PaginatedRemover<CustomerId, Asset>() {

        @Override
        protected PageData<Asset> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Asset entity) {
            unassignAssetFromCustomer(tenantId, new AssetId(entity.getId().getId()));
        }
    };
}
