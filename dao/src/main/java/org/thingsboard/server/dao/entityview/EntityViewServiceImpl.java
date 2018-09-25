/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.ENTITY_VIEW_CACHE;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
@Service
@Slf4j
public class EntityViewServiceImpl extends AbstractEntityService implements EntityViewService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_ENTITY_VIEW_ID = "Incorrect entityViewId ";
    private static final int DEFAULT_LIMIT = 100;

    @Autowired
    private EntityViewDao entityViewDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private CacheManager cacheManager;

    @CacheEvict(cacheNames = ENTITY_VIEW_CACHE, key = "{#entityView.tenantId, #entityView.name}")
    @Override
    public EntityView saveEntityView(EntityView entityView) {
        log.trace("Executing save entity view [{}]", entityView);
        entityViewValidator.validate(entityView);
        EntityView savedEntityView = entityViewDao.save(entityView);
        if (savedEntityView.getKeys() != null) {
            copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.CLIENT_SCOPE, savedEntityView.getKeys().getAttributes().getCs());
            copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SERVER_SCOPE, savedEntityView.getKeys().getAttributes().getSs());
            copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SHARED_SCOPE, savedEntityView.getKeys().getAttributes().getSh());
        }
        return savedEntityView;
    }

    @Override
    public EntityView assignEntityViewToCustomer(EntityViewId entityViewId, CustomerId customerId) {
        EntityView entityView = findEntityViewById(entityViewId);
        entityView.setCustomerId(customerId);
        return saveEntityView(entityView);
    }

    @Override
    public EntityView unassignEntityViewFromCustomer(EntityViewId entityViewId) {
        EntityView entityView = findEntityViewById(entityViewId);
        entityView.setCustomerId(null);
        return saveEntityView(entityView);
    }

    @Override
    public void unassignCustomerEntityViews(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerEntityViews, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        new CustomerEntityViewsUnAssigner(tenantId).removeEntities(customerId);
    }


    @Override
    public EntityView findEntityViewById(EntityViewId entityViewId) {
        log.trace("Executing findEntityViewById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        List<Object> ids = Arrays.asList(entityViewId.getId());
        Cache cache = cacheManager.getCache(ENTITY_VIEW_CACHE);
        EntityView fromCache = cache.get(ids, EntityView.class);
        if (fromCache != null) {
            return fromCache;
        } else {
            ListenableFuture<EntityView> entityViewFuture
                    = Futures.immediateFuture(entityViewDao.findById(entityViewId.getId()));
            Futures.addCallback(entityViewFuture,
                    new FutureCallback<EntityView>() {
                        @Override
                        public void onSuccess(@Nullable EntityView result) {
                            cache.putIfAbsent(ids, result);
                        }
                        @Override
                        public void onFailure(Throwable t) {}
                    });
            try {
                return entityViewFuture.get();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return entityViewDao.findById(entityViewId.getId());
    }

    @Override
    public TextPageData<EntityView> findEntityViewByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findEntityViewsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<EntityView> entityViews = entityViewDao.findEntityViewsByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(entityViews, pageLink);
    }

    @Override
    public TextPageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId,
                                                                          TextPageLink pageLink) {
        log.trace("Executing findEntityViewByTenantIdAndCustomerId, tenantId [{}], customerId [{}]," +
                        " pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<EntityView> entityViews = entityViewDao.findEntityViewsByTenantIdAndCustomerId(tenantId.getId(),
                customerId.getId(), pageLink);
        return new TextPageData<>(entityViews, pageLink);
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByQuery(EntityViewSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(query.toEntitySearchQuery());
        ListenableFuture<List<EntityView>> entityViews = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<EntityView>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.ENTITY_VIEW) {
                    futures.add(findEntityViewByIdAsync(new EntityViewId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        });

        return entityViews;
    }

    @Override
    public ListenableFuture<EntityView> findEntityViewByIdAsync(EntityViewId entityViewId) {
        log.trace("Executing findEntityViewById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return entityViewDao.findByIdAsync(entityViewId.getId());
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findEntityViewsByTenantIdAndEntityIdAsync, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityId.getId(), "Incorrect entityId" + entityId);

        List<Object> tenantAndEntityIds = Arrays.asList(tenantId, entityId);
        Cache cache = cacheManager.getCache(ENTITY_VIEW_CACHE);
        List<EntityView> fromCache = cache.get(tenantAndEntityIds, List.class);
        if (fromCache != null) {
            return Futures.immediateFuture(fromCache);
        } else {
            ListenableFuture<List<EntityView>> entityViewsFuture =
                    entityViewDao.findEntityViewsByTenantIdAndEntityIdAsync(tenantId.getId(), entityId.getId());
            Futures.addCallback(entityViewsFuture,
                    new FutureCallback<List<EntityView>>() {
                        @Override
                        public void onSuccess(@Nullable List<EntityView> result) {
                            cache.putIfAbsent(tenantAndEntityIds, result);
                        }
                        @Override
                        public void onFailure(Throwable t) {}
                    });
            return entityViewsFuture;
        }
    }

    @Override
    public void deleteEntityView(EntityViewId entityViewId) {
        log.trace("Executing deleteEntityView [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        deleteEntityRelations(entityViewId);
        cacheEvict(entityViewId, cacheManager.getCache(ENTITY_VIEW_CACHE));
        entityViewDao.removeById(entityViewId.getId());
    }

    @Override
    public void deleteEntityViewsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEntityViewsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        entityViewDao.findEntityViewsByTenantId(tenantId.getId(), new TextPageLink(DEFAULT_LIMIT)).stream()
                .map(view ->  view.getId())
                .collect(Collectors.toList())
                .forEach(id -> cacheEvict(id, cacheManager.getCache(ENTITY_VIEW_CACHE)));
        tenantEntityViewRemover.removeEntities(tenantId);
    }

    private void cacheEvict(EntityViewId entityViewId, Cache cache) {
        EntityView entityView = entityViewDao.findById(entityViewId.getId());
        cache.evict(Arrays.asList(entityView.getTenantId(), entityView.getName()));
        cache.evict(Arrays.asList(entityView.getTenantId(), entityView.getEntityId()));
    }

    private void copyAttributesFromEntityToEntityView(EntityView entityView, String scope, Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<AttributeKvEntry>> getAttrFuture = attributesService.find(entityView.getEntityId(), scope, keys);
            Futures.addCallback(getAttrFuture, new FutureCallback<List<AttributeKvEntry>>() {
                @Override
                public void onSuccess(@Nullable List<AttributeKvEntry> attributeKvEntries) {
                    if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
                        List<AttributeKvEntry> filteredAttributes =
                                attributeKvEntries.stream()
                                        .filter(attributeKvEntry -> {
                                            if (entityView.getStartTimeMs() == 0 && entityView.getEndTimeMs() == 0) {
                                                return true;
                                            }
                                            if (entityView.getEndTimeMs() == 0 && entityView.getStartTimeMs() < attributeKvEntry.getLastUpdateTs()) {
                                                return true;
                                            }
                                            if (entityView.getStartTimeMs() == 0 && entityView.getEndTimeMs() > attributeKvEntry.getLastUpdateTs()) {
                                                return true;
                                            }
                                            return entityView.getStartTimeMs() < attributeKvEntry.getLastUpdateTs()
                                                    && entityView.getEndTimeMs() > attributeKvEntry.getLastUpdateTs();
                                        }).collect(Collectors.toList());
                        attributesService.save(entityView.getId(), scope, filteredAttributes);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("Failed to fetch [{}] attributes [{}] for [{}] entity [{}]",
                            scope, keys, entityView.getEntityId().getEntityType().name(), entityView.getEntityId().getId().toString(), throwable);
                }
            });
        }
    }

    private DataValidator<EntityView> entityViewValidator =
            new DataValidator<EntityView>() {

                @Override
                protected void validateCreate(EntityView entityView) {
                    entityViewDao.findEntityViewByTenantIdAndName(entityView.getTenantId().getId(), entityView.getName())
                            .ifPresent( e -> {
                                throw new DataValidationException("Entity view with such name already exists!");
                            });
                }

                @Override
                protected void validateUpdate(EntityView entityView) {
                    entityViewDao.findEntityViewByTenantIdAndName(entityView.getTenantId().getId(), entityView.getName())
                            .ifPresent( e -> {
                                if (!e.getUuidId().equals(entityView.getUuidId())) {
                                    throw new DataValidationException("Entity view with such name already exists!");
                                }
                            });
                }

                @Override
                protected void validateDataImpl(EntityView entityView) {
                    if (StringUtils.isEmpty(entityView.getName())) {
                        throw new DataValidationException("Entity view name should be specified!");
                    }
                    if (entityView.getTenantId() == null) {
                        throw new DataValidationException("Entity view should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(entityView.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Entity view is referencing to non-existent tenant!");
                        }
                    }
                    if (entityView.getCustomerId() == null) {
                        entityView.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!entityView.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(entityView.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign entity view to non-existent customer!");
                        }
                        if (!customer.getTenantId().getId().equals(entityView.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign entity view to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, EntityView> tenantEntityViewRemover =
            new PaginatedRemover<TenantId, EntityView>() {

                @Override
                protected List<EntityView> findEntities(TenantId id, TextPageLink pageLink) {
                    return entityViewDao.findEntityViewsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(EntityView entity) {
                    deleteEntityView(new EntityViewId(entity.getUuidId()));
                }
            };

    private class CustomerEntityViewsUnAssigner extends PaginatedRemover<CustomerId, EntityView> {

        private TenantId tenantId;

        CustomerEntityViewsUnAssigner(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        protected List<EntityView> findEntities(CustomerId id, TextPageLink pageLink) {
            return entityViewDao.findEntityViewsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(EntityView entity) {
            unassignEntityViewFromCustomer(new EntityViewId(entity.getUuidId()));
        }
    }
}
