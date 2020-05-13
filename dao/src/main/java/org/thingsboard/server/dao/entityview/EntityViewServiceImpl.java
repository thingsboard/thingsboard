/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.ENTITY_VIEW_CACHE;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

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

    @Autowired
    private EntityViewDao entityViewDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private CacheManager cacheManager;

    @Caching(evict = {
            @CacheEvict(cacheNames = ENTITY_VIEW_CACHE, key = "{#entityView.tenantId, #entityView.entityId}"),
            @CacheEvict(cacheNames = ENTITY_VIEW_CACHE, key = "{#entityView.tenantId, #entityView.name}"),
            @CacheEvict(cacheNames = ENTITY_VIEW_CACHE, key = "{#entityView.id}")})
    @Override
    public EntityView saveEntityView(EntityView entityView) {
        log.trace("Executing save entity view [{}]", entityView);
        entityViewValidator.validate(entityView, EntityView::getTenantId);
        EntityView savedEntityView = entityViewDao.save(entityView.getTenantId(), entityView);
        return savedEntityView;
    }

    @CacheEvict(cacheNames = ENTITY_VIEW_CACHE, key = "{#entityViewId}")
    @Override
    public EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, CustomerId customerId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        entityView.setCustomerId(customerId);
        return saveEntityView(entityView);
    }

    @CacheEvict(cacheNames = ENTITY_VIEW_CACHE, key = "{#entityViewId}")
    @Override
    public EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        entityView.setCustomerId(null);
        return saveEntityView(entityView);
    }

    @Override
    public void unassignCustomerEntityViews(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerEntityViews, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerEntityViewsUnAssigner.removeEntities(tenantId, customerId);
    }

    @Override
    public EntityViewInfo findEntityViewInfoById(TenantId tenantId, EntityViewId entityViewId) {
        log.trace("Executing findEntityViewInfoById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return entityViewDao.findEntityViewInfoById(tenantId, entityViewId.getId());
    }

    @Cacheable(cacheNames = ENTITY_VIEW_CACHE, key = "{#entityViewId}")
    @Override
    public EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId) {
        log.trace("Executing findEntityViewById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return entityViewDao.findById(tenantId, entityViewId.getId());
    }

    @Cacheable(cacheNames = ENTITY_VIEW_CACHE, key = "{#tenantId, #name}")
    @Override
    public EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findEntityViewByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Optional<EntityView> entityViewOpt = entityViewDao.findEntityViewByTenantIdAndName(tenantId.getId(), name);
        return entityViewOpt.orElse(null);
    }

    @Override
    public PageData<EntityView> findEntityViewByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEntityViewsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewByTenantIdAndType(TenantId tenantId, PageLink pageLink, String type) {
        log.trace("Executing findEntityViewByTenantIdAndType, tenantId [{}], pageLink [{}], type [{}]", tenantId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndType, tenantId [{}], pageLink [{}], type [{}]", tenantId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId,
                                                                       PageLink pageLink) {
        log.trace("Executing findEntityViewByTenantIdAndCustomerId, tenantId [{}], customerId [{}]," +
                " pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantIdAndCustomerId(tenantId.getId(),
                customerId.getId(), pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}]," +
                " pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewInfosByTenantIdAndCustomerId(tenantId.getId(),
                customerId.getId(), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, PageLink pageLink, String type) {
        log.trace("Executing findEntityViewsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}]," +
                " pageLink [{}], type [{}]", tenantId, customerId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewsByTenantIdAndCustomerIdAndType(tenantId.getId(),
                customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}]," +
                " pageLink [{}], type [{}]", tenantId, customerId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewInfosByTenantIdAndCustomerIdAndType(tenantId.getId(),
                customerId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByQuery(TenantId tenantId, EntityViewSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<EntityView>> entityViews = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<EntityView>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.ENTITY_VIEW) {
                    futures.add(findEntityViewByIdAsync(tenantId, new EntityViewId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());

        entityViews = Futures.transform(entityViews, new Function<List<EntityView>, List<EntityView>>() {
            @Nullable
            @Override
            public List<EntityView> apply(@Nullable List<EntityView> entityViewList) {
                return entityViewList == null ? Collections.emptyList() : entityViewList.stream().filter(entityView -> query.getEntityViewTypes().contains(entityView.getType())).collect(Collectors.toList());
            }
        }, MoreExecutors.directExecutor());

        return entityViews;
    }

    @Override
    public ListenableFuture<EntityView> findEntityViewByIdAsync(TenantId tenantId, EntityViewId entityViewId) {
        log.trace("Executing findEntityViewById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return entityViewDao.findByIdAsync(tenantId, entityViewId.getId());
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findEntityViewsByTenantIdAndEntityIdAsync, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityId.getId(), "Incorrect entityId" + entityId);

        List<Object> tenantIdAndEntityId = new ArrayList<>();
        tenantIdAndEntityId.add(tenantId);
        tenantIdAndEntityId.add(entityId);

        Cache cache = cacheManager.getCache(ENTITY_VIEW_CACHE);
        List<EntityView> fromCache = cache.get(tenantIdAndEntityId, List.class);
        if (fromCache != null) {
            return Futures.immediateFuture(fromCache);
        } else {
            ListenableFuture<List<EntityView>> entityViewsFuture = entityViewDao.findEntityViewsByTenantIdAndEntityIdAsync(tenantId.getId(), entityId.getId());
            Futures.addCallback(entityViewsFuture,
                    new FutureCallback<List<EntityView>>() {
                        @Override
                        public void onSuccess(@Nullable List<EntityView> result) {
                            cache.putIfAbsent(tenantIdAndEntityId, result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("Error while finding entity views by tenantId and entityId", t);
                        }
                    }, MoreExecutors.directExecutor());
            return entityViewsFuture;
        }
    }

    @CacheEvict(cacheNames = ENTITY_VIEW_CACHE, key = "{#entityViewId}")
    @Override
    public void deleteEntityView(TenantId tenantId, EntityViewId entityViewId) {
        log.trace("Executing deleteEntityView [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        deleteEntityRelations(tenantId, entityViewId);
        EntityView entityView = entityViewDao.findById(tenantId, entityViewId.getId());
        cacheManager.getCache(ENTITY_VIEW_CACHE).evict(Arrays.asList(entityView.getTenantId(), entityView.getEntityId()));
        cacheManager.getCache(ENTITY_VIEW_CACHE).evict(Arrays.asList(entityView.getTenantId(), entityView.getName()));
        entityViewDao.removeById(tenantId, entityViewId.getId());
    }

    @Override
    public void deleteEntityViewsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEntityViewsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantEntityViewRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findEntityViewTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findEntityViewTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantEntityViewTypes = entityViewDao.findTenantEntityViewTypesAsync(tenantId.getId());
        return Futures.transform(tenantEntityViewTypes,
                entityViewTypes -> {
                    entityViewTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return entityViewTypes;
                }, MoreExecutors.directExecutor());
    }

    private DataValidator<EntityView> entityViewValidator =
            new DataValidator<EntityView>() {

                @Override
                protected void validateCreate(TenantId tenantId, EntityView entityView) {
                    entityViewDao.findEntityViewByTenantIdAndName(entityView.getTenantId().getId(), entityView.getName())
                            .ifPresent(e -> {
                                throw new DataValidationException("Entity view with such name already exists!");
                            });
                }

                @Override
                protected void validateUpdate(TenantId tenantId, EntityView entityView) {
                    entityViewDao.findEntityViewByTenantIdAndName(entityView.getTenantId().getId(), entityView.getName())
                            .ifPresent(e -> {
                                if (!e.getUuidId().equals(entityView.getUuidId())) {
                                    throw new DataValidationException("Entity view with such name already exists!");
                                }
                            });
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, EntityView entityView) {
                    if (StringUtils.isEmpty(entityView.getType())) {
                        throw new DataValidationException("Entity View type should be specified!");
                    }
                    if (StringUtils.isEmpty(entityView.getName())) {
                        throw new DataValidationException("Entity view name should be specified!");
                    }
                    if (entityView.getTenantId() == null) {
                        throw new DataValidationException("Entity view should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, entityView.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Entity view is referencing to non-existent tenant!");
                        }
                    }
                    if (entityView.getCustomerId() == null) {
                        entityView.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!entityView.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(tenantId, entityView.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign entity view to non-existent customer!");
                        }
                        if (!customer.getTenantId().getId().equals(entityView.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign entity view to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, EntityView> tenantEntityViewRemover = new PaginatedRemover<TenantId, EntityView>() {
        @Override
        protected PageData<EntityView> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return entityViewDao.findEntityViewsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, EntityView entity) {
            deleteEntityView(tenantId, new EntityViewId(entity.getUuidId()));
        }
    };

    private PaginatedRemover<CustomerId, EntityView> customerEntityViewsUnAssigner = new PaginatedRemover<CustomerId, EntityView>() {
        @Override
        protected PageData<EntityView> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return entityViewDao.findEntityViewsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, EntityView entity) {
            unassignEntityViewFromCustomer(tenantId, new EntityViewId(entity.getUuidId()));
        }
    };
}
