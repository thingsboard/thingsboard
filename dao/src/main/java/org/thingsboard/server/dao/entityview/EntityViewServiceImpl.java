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
package org.thingsboard.server.dao.entityview;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteDaoEvent;
import org.thingsboard.server.dao.eventsourcing.EntityUpdateEvent;
import org.thingsboard.server.dao.eventsourcing.SaveDaoEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
@Service("EntityViewDaoService")
@Slf4j
public class EntityViewServiceImpl extends AbstractCachedEntityService<EntityViewCacheKey, EntityViewCacheValue, EntityViewEvictEvent> implements EntityViewService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_ENTITY_VIEW_ID = "Incorrect entityViewId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";

    @Autowired
    private EntityViewDao entityViewDao;

    @Autowired
    private DataValidator<EntityView> entityViewValidator;

    @Autowired
    protected JpaExecutorService service;

    @TransactionalEventListener(classes = EntityViewEvictEvent.class)
    @Override
    public void handleEvictEvent(EntityViewEvictEvent event) {
        List<EntityViewCacheKey> keys = new ArrayList<>(5);
        keys.add(EntityViewCacheKey.byName(event.getTenantId(), event.getNewName()));
        keys.add(EntityViewCacheKey.byId(event.getId()));
        keys.add(EntityViewCacheKey.byEntityId(event.getTenantId(), event.getNewEntityId()));
        if (event.getOldEntityId() != null && !event.getOldEntityId().equals(event.getNewEntityId())) {
            keys.add(EntityViewCacheKey.byEntityId(event.getTenantId(), event.getOldEntityId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(EntityViewCacheKey.byName(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    public EntityView saveEntityView(EntityView entityView) {
        log.trace("Executing save entity view [{}]", entityView);
        EntityView old = entityViewValidator.validate(entityView, EntityView::getTenantId);
        try {
            EntityView saved = entityViewDao.save(entityView.getTenantId(), entityView);
            publishEvictEvent(new EntityViewEvictEvent(saved.getTenantId(), saved.getId(), saved.getEntityId(), old != null ? old.getEntityId() : null, saved.getName(), old != null ? old.getName() : null));
            eventPublisher.publishEvent(SaveDaoEvent.builder().tenantId(saved.getTenantId()).entityId(saved.getId()).entity(saved).build());
            return saved;
        } catch (Exception t) {
            checkConstraintViolation(t,
                    "entity_view_external_id_unq_key", "Entity View with such external id already exists!");
            throw t;
        }
    }

    @Override
    public EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, CustomerId customerId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        entityView.setCustomerId(customerId);
        eventPublisher.publishEvent(new EntityUpdateEvent(tenantId, null, entityViewId,
                JacksonUtil.toString(customerId), EdgeEventActionType.ASSIGNED_TO_CUSTOMER));
        return saveEntityView(entityView);
    }

    @Override
    public EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        CustomerId customerId = entityView.getCustomerId();
        entityView.setCustomerId(null);
        eventPublisher.publishEvent(new EntityUpdateEvent(tenantId, null, entityViewId,
                JacksonUtil.toString(customerId), EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER));
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

    @Override
    public EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId) {
        log.trace("Executing findEntityViewById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return cache.getAndPutInTransaction(EntityViewCacheKey.byId(entityViewId),
                () -> entityViewDao.findById(tenantId, entityViewId.getId())
                , EntityViewCacheValue::getEntityView, v -> new EntityViewCacheValue(v, null), true);
    }

    @Override
    public EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findEntityViewByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(EntityViewCacheKey.byName(tenantId, name),
                () -> entityViewDao.findEntityViewByTenantIdAndName(tenantId.getId(), name).orElse(null)
                , EntityViewCacheValue::getEntityView, v -> new EntityViewCacheValue(v, null), true);

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

        return service.submit(() -> cache.getAndPutInTransaction(EntityViewCacheKey.byEntityId(tenantId, entityId),
                () -> entityViewDao.findEntityViewsByTenantIdAndEntityId(tenantId.getId(), entityId.getId()),
                EntityViewCacheValue::getEntityViews, v -> new EntityViewCacheValue(null, v), true));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findEntityViewsByTenantIdAndEntityId, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityId.getId(), "Incorrect entityId" + entityId);

        return cache.getAndPutInTransaction(EntityViewCacheKey.byEntityId(tenantId, entityId),
                () -> entityViewDao.findEntityViewsByTenantIdAndEntityId(tenantId.getId(), entityId.getId()),
                EntityViewCacheValue::getEntityViews, v -> new EntityViewCacheValue(null, v), true);
    }

    @Override
    @Transactional
    public void deleteEntityView(TenantId tenantId, EntityViewId entityViewId) {
        log.trace("Executing deleteEntityView [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        publishDeleteEntityView(tenantId, entityViewId);
        deleteEntityRelations(tenantId, entityViewId);
        EntityView entityView = entityViewDao.findById(tenantId, entityViewId.getId());
        entityViewDao.removeById(tenantId, entityViewId.getId());
        publishEvictEvent(new EntityViewEvictEvent(entityView.getTenantId(), entityView.getId(), entityView.getEntityId(), null, entityView.getName(), null));
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

    @Override
    public EntityView assignEntityViewToEdge(TenantId tenantId, EntityViewId entityViewId, EdgeId edgeId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign entityView to non-existent edge!");
        }
        if (!edge.getTenantId().getId().equals(entityView.getTenantId().getId())) {
            throw new DataValidationException("Can't assign entityView to edge from different tenant!");
        }

        boolean relationExists = relationService.checkRelation(tenantId, edgeId, entityView.getEntityId(),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        if (!relationExists) {
            throw new DataValidationException("Can't assign entity view to edge because related device/asset doesn't assigned to edge!");
        }

        try {
            createRelation(tenantId, new EntityRelation(edgeId, entityViewId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create entityView relation. Edge Id: [{}]", entityViewId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(new EntityUpdateEvent(tenantId, edgeId, entityViewId,
                null, EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER));
        return entityView;
    }

    @Override
    public EntityView unassignEntityViewFromEdge(TenantId tenantId, EntityViewId entityViewId, EdgeId edgeId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign entityView from non-existent edge!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, entityViewId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete entityView relation. Edge Id: [{}]", entityViewId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(new EntityUpdateEvent(tenantId, edgeId, entityViewId,
                null, EdgeEventActionType.UNASSIGNED_FROM_EDGE));
        return entityView;
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findEntityViewsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink) {
        log.trace("Executing findEntityViewsByTenantIdAndEdgeIdAndType, tenantId [{}], edgeId [{}], type [{}], pageLink [{}]", tenantId, edgeId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantIdAndEdgeIdAndType(tenantId.getId(), edgeId.getId(), type, pageLink);
    }

    private void publishDeleteEntityView(TenantId tenantId, EntityViewId entityViewId) {
        List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, entityViewId);
        if (relatedEdgeIds != null && !relatedEdgeIds.isEmpty()) {
            eventPublisher.publishEvent(DeleteDaoEvent.builder().tenantId(tenantId).entityId(entityViewId).entity(entityViewId).relatedEdgeIds(relatedEdgeIds));
        }
    }

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

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findEntityViewById(tenantId, new EntityViewId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }

}
