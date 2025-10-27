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
package org.thingsboard.server.dao.edge;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.edge.EdgeCacheEvictEvent;
import org.thingsboard.server.cache.edge.EdgeCacheKey;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.edge.BaseRelatedEdgesService.RELATED_EDGES_CACHE_ITEMS;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("EdgeDaoService")
@Slf4j
public class EdgeServiceImpl extends AbstractCachedEntityService<EdgeCacheKey, Edge, EdgeCacheEvictEvent> implements EdgeService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";
    public static final String EDGE_IS_ROOT_BODY_KEY = "isRoot";

    @Autowired
    private EdgeDao edgeDao;

    @Autowired
    private UserService userService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    @Lazy
    private TimeseriesService timeseriesService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private DataValidator<Edge> edgeValidator;

    @Autowired
    private JpaExecutorService executor;

    @Autowired
    @Lazy
    private RelatedEdgesService relatedEdgesService;

    @Autowired
    private EntityCountService countService;

    @Value("${edges.enabled}")
    @Getter
    private boolean edgesEnabled;
    @Value("${edges.state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    @TransactionalEventListener(classes = EdgeCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(EdgeCacheEvictEvent event) {
        List<EdgeCacheKey> keys = new ArrayList<>(2);
        keys.add(new EdgeCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new EdgeCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    public Edge findEdgeById(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeById [{}]", edgeId);
        validateId(edgeId, id -> INCORRECT_EDGE_ID + id);
        return edgeDao.findById(tenantId, edgeId.getId());
    }

    @Override
    public EdgeInfo findEdgeInfoById(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeInfoById [{}]", edgeId);
        validateId(edgeId, id -> INCORRECT_EDGE_ID + id);
        return edgeDao.findEdgeInfoById(tenantId, edgeId.getId());
    }

    @Override
    public ListenableFuture<Edge> findEdgeByIdAsync(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeByIdAsync [{}]", edgeId);
        validateId(edgeId, id -> INCORRECT_EDGE_ID + id);
        return edgeDao.findByIdAsync(tenantId, edgeId.getId());
    }

    @Override
    public Edge findEdgeByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findEdgeByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return cache.getAndPutInTransaction(new EdgeCacheKey(tenantId, name),
                () -> edgeDao.findEdgeByTenantIdAndName(tenantId.getId(), name)
                        .orElse(null), true);
    }

    @Override
    public ListenableFuture<Edge> findEdgeByTenantIdAndNameAsync(TenantId tenantId, String name) {
        log.trace("Executing findEdgeByTenantIdAndNameAsync [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return executor.submit(() -> findEdgeByTenantIdAndName(tenantId, name));
    }

    @Override
    public Optional<Edge> findEdgeByRoutingKey(TenantId tenantId, String routingKey) {
        log.trace("Executing findEdgeByRoutingKey [{}]", routingKey);
        Validator.validateString(routingKey, "Incorrect edge routingKey for search request.");
        return edgeDao.findByRoutingKey(tenantId.getId(), routingKey);
    }

    @Override
    public PageData<Edge> findActiveEdges(PageLink pageLink) {
        log.trace("Executing findActiveEdges [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return edgeDao.findActiveEdges(pageLink);
    }

    @Override
    public Edge saveEdge(Edge edge) {
        return saveEntity(edge, () -> doSaveEdge(edge));
    }

    private Edge doSaveEdge(Edge edge) {
        log.trace("Executing saveEdge [{}]", edge);
        Edge oldEdge = edgeValidator.validate(edge, Edge::getTenantId);
        EdgeCacheEvictEvent evictEvent = new EdgeCacheEvictEvent(edge.getTenantId(), edge.getName(), oldEdge != null ? oldEdge.getName() : null);
        try {
            Edge savedEdge = edgeDao.save(edge.getTenantId(), edge);
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedEdge.getTenantId())
                    .entityId(savedEdge.getId()).entity(savedEdge).created(edge.getId() == null).build());
            if (edge.getId() == null) {
                countService.publishCountEntityEvictEvent(savedEdge.getTenantId(), EntityType.EDGE);
            }
            return savedEdge;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null
                    && e.getConstraintName().equalsIgnoreCase("edge_name_unq_key")) {
                throw new DataValidationException("Edge with such name already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public Edge assignEdgeToCustomer(TenantId tenantId, EdgeId edgeId, CustomerId customerId) {
        log.trace("[{}] Executing assignEdgeToCustomer [{}][{}]", tenantId, edgeId, customerId);
        Edge edge = findEdgeById(tenantId, edgeId);
        if (customerId.equals(edge.getCustomerId())) {
            return edge;
        }
        edge.setCustomerId(customerId);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(edgeId)
                .body(JacksonUtil.toString(customerId)).actionType(ActionType.ASSIGNED_TO_CUSTOMER).build());
        return saveEdge(edge);
    }

    @Override
    public Edge unassignEdgeFromCustomer(TenantId tenantId, EdgeId edgeId) {
        log.trace("[{}] Executing unassignEdgeFromCustomer [{}]", tenantId, edgeId);
        Edge edge = findEdgeById(tenantId, edgeId);
        CustomerId customerId = edge.getCustomerId();
        if (customerId == null) {
            return edge;
        }
        edge.setCustomerId(null);
        Edge result = saveEdge(edge);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(edgeId)
                .body(JacksonUtil.toString(customerId)).actionType(ActionType.UNASSIGNED_FROM_CUSTOMER).build());
        return result;
    }

    @Override
    @Transactional
    public void deleteEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing deleteEdge [{}]", edgeId);
        validateId(edgeId, id -> INCORRECT_EDGE_ID + id);

        Edge edge = edgeDao.findById(tenantId, edgeId.getId());
        if (edge == null) {
            return;
        }
        edgeDao.removeById(tenantId, edgeId.getId());

        publishEvictEvent(new EdgeCacheEvictEvent(edge.getTenantId(), edge.getName(), null));
        countService.publishCountEntityEvictEvent(tenantId, EntityType.EDGE);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(edgeId).build());
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteEdge(tenantId, (EdgeId) id);
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return edgeDao.findEdgeIdsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(TenantId tenantId, List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdAndIdsAsync, tenantId [{}], edgeIds [{}]", tenantId, edgeIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(edgeIds, ids -> "Incorrect edgeIds " + ids);
        return edgeDao.findEdgesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(edgeIds));
    }

    @Override
    public void deleteEdgesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEdgesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantEdgesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteEdgesByTenantId(tenantId);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], edgeIds [{}]", tenantId, customerId, edgeIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateIds(edgeIds, ids -> "Incorrect edgeIds " + ids);
        return edgeDao.findEdgesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(edgeIds));
    }

    @Override
    public void unassignCustomerEdges(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerEdges, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        customerEdgeRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByQuery(TenantId tenantId, EdgeSearchQuery query) {
        log.trace("[{}] Executing findEdgesByQuery [{}]", tenantId, query);
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<Edge>> edges = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Edge>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.EDGE) {
                    futures.add(findEdgeByIdAsync(tenantId, new EdgeId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());

        edges = Futures.transform(edges, edgeList -> edgeList == null ?
                Collections.emptyList() :
                edgeList.stream().filter(edge -> query.getEdgeTypes().contains(edge.getType())).collect(Collectors.toList()), MoreExecutors.directExecutor());
        return edges;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findEdgeTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findEdgeTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        ListenableFuture<List<EntitySubtype>> tenantEdgeTypes = edgeDao.findTenantEdgeTypesAsync(tenantId.getId());
        return Futures.transform(tenantEdgeTypes,
                edgeTypes -> {
                    edgeTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return edgeTypes;
                }, MoreExecutors.directExecutor());
    }

    @Override
    public void assignDefaultRuleChainsToEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing assignDefaultRuleChainsToEdge, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        PageDataIterable<RuleChain> ruleChains = new PageDataIterable<>(
                link -> ruleChainService.findAutoAssignToEdgeRuleChainsByTenantId(tenantId, link), 1024);
        for (RuleChain ruleChain : ruleChains) {
            ruleChainService.assignRuleChainToEdge(tenantId, ruleChain.getId(), edgeId);
        }
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndEntityId(tenantId.getId(), entityId.getId(), entityId.getEntityType(), pageLink);
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByTenantIdAndEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return edgeDao.findEdgeIdsByTenantIdAndEntityId(tenantId.getId(), entityId.getId(), entityId.getEntityType(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantProfileId(TenantProfileId tenantProfileId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantProfileId, tenantProfileId [{}], pageLink [{}]", tenantProfileId, pageLink);
        Validator.validateId(tenantProfileId, id -> "Incorrect tenantProfileId " + id);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantProfileId(tenantProfileId.getId(), pageLink);
    }

    private final PaginatedRemover<TenantId, Edge> tenantEdgesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Edge> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return edgeDao.findEdgesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Edge entity) {
                    deleteEdge(tenantId, new EdgeId(entity.getUuidId()));
                }
            };

    private final PaginatedRemover<CustomerId, Edge> customerEdgeRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Edge> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return edgeDao.findEdgesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Edge entity) {
            unassignEdgeFromCustomer(tenantId, new EdgeId(entity.getUuidId()));
        }
    };

    @Override
    public List<EdgeId> findAllRelatedEdgeIds(TenantId tenantId, EntityId entityId) {
        if (!edgesEnabled) {
            return null;
        }
        if (EntityType.EDGE.equals(entityId.getEntityType())) {
            return Collections.singletonList(new EdgeId(entityId.getId()));
        }
        PageDataIterableByTenantIdEntityId<EdgeId> relatedEdgeIdsIterator =
                new PageDataIterableByTenantIdEntityId<>(this::findRelatedEdgeIdsByEntityId, tenantId, entityId, RELATED_EDGES_CACHE_ITEMS);
        List<EdgeId> result = new ArrayList<>();
        for (EdgeId edgeId : relatedEdgeIdsIterator) {
            result.add(edgeId);
        }
        return result;
    }

    @Override
    public PageData<EdgeId> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("[{}] Executing findRelatedEdgeIdsByEntityId [{}] [{}]", tenantId, entityId, pageLink);
        switch (entityId.getEntityType()) {
            case TENANT:
            case DEVICE_PROFILE:
            case ASSET_PROFILE:
            case OTA_PACKAGE:
                return convertToEdgeIds(findEdgesByTenantId(tenantId, pageLink));
            case CUSTOMER:
                return convertToEdgeIds(findEdgesByTenantIdAndCustomerId(tenantId, new CustomerId(entityId.getId()), pageLink));
            case EDGE:
                return new PageData<>(List.of(new EdgeId(entityId.getId())), 1, 1, false);
            case DEVICE:
            case ASSET:
            case ENTITY_VIEW:
            case DASHBOARD:
            case RULE_CHAIN:
                return relatedEdgesService.findEdgeIdsByEntityId(tenantId, entityId, pageLink);
            case USER:
                User userById = userService.findUserById(tenantId, new UserId(entityId.getId()));
                if (userById == null) {
                    return PageData.emptyPageData();
                }
                if (userById.getCustomerId() == null || userById.getCustomerId().isNullUid()) {
                    return convertToEdgeIds(findEdgesByTenantId(tenantId, pageLink));
                } else {
                    return convertToEdgeIds(findEdgesByTenantIdAndCustomerId(tenantId, userById.getCustomerId(), pageLink));
                }
            case TENANT_PROFILE:
                return convertToEdgeIds(findEdgesByTenantProfileId(new TenantProfileId(entityId.getId()), pageLink));
            default:
                log.warn("[{}] Unsupported entity type {}", tenantId, entityId.getEntityType());
                return PageData.emptyPageData();
        }
    }

    private PageData<EdgeId> convertToEdgeIds(PageData<Edge> pageData) {
        if (pageData == null) {
            return PageData.emptyPageData();
        }
        List<EdgeId> edgeIds = new ArrayList<>();
        if (pageData.getData() != null && !pageData.getData().isEmpty()) {
            edgeIds = pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        }
        return new PageData<>(edgeIds, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

    @Override
    public String findMissingToRelatedRuleChains(TenantId tenantId, EdgeId edgeId, String tbRuleChainInputNodeClassName) {
        List<RuleChain> edgeRuleChains = findEdgeRuleChains(tenantId, edgeId);
        List<RuleChainId> edgeRuleChainIds = edgeRuleChains.stream().map(IdBased::getId).toList();
        ObjectNode result = JacksonUtil.newObjectNode();
        for (RuleChain edgeRuleChain : edgeRuleChains) {
            List<RuleNode> ruleNodes =
                    ruleChainService.loadRuleChainMetaData(edgeRuleChain.getTenantId(), edgeRuleChain.getId()).getNodes();
            if (ruleNodes != null && !ruleNodes.isEmpty()) {
                List<RuleChainId> connectedRuleChains =
                        ruleNodes.stream()
                                .filter(rn -> rn.getType().equals(tbRuleChainInputNodeClassName))
                                .map(rn -> new RuleChainId(UUID.fromString(rn.getConfiguration().get("ruleChainId").asText()))).toList();
                List<String> missingRuleChains = new ArrayList<>();
                for (RuleChainId connectedRuleChain : connectedRuleChains) {
                    if (!edgeRuleChainIds.contains(connectedRuleChain)) {
                        RuleChain ruleChainById = ruleChainService.findRuleChainById(tenantId, connectedRuleChain);
                        missingRuleChains.add(ruleChainById.getName());
                    }
                }
                if (!missingRuleChains.isEmpty()) {
                    ArrayNode array = JacksonUtil.newArrayNode();
                    for (String missingRuleChain : missingRuleChains) {
                        array.add(missingRuleChain);
                    }
                    result.set(edgeRuleChain.getName(), array);
                }
            }
        }
        return result.toString();
    }

    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = saveEdge(edge);
        ObjectNode isRootBody = JacksonUtil.newObjectNode();
        isRootBody.put(EDGE_IS_ROOT_BODY_KEY, Boolean.TRUE);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edge.getId()).entityId(ruleChainId)
                .body(JacksonUtil.toString(isRootBody)).actionType(ActionType.UPDATED).build());
        return savedEdge;
    }

    @Override
    public ListenableFuture<Boolean> isEdgeActiveAsync(TenantId tenantId, EdgeId edgeId, String key) {
        ListenableFuture<? extends Optional<? extends KvEntry>> futureKvEntry;
        if (persistToTelemetry) {
            futureKvEntry = timeseriesService.findLatest(tenantId, edgeId, key);
        } else {
            futureKvEntry = attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, key);
        }
        return Futures.transformAsync(futureKvEntry, kvEntryOpt ->
                Futures.immediateFuture(kvEntryOpt.flatMap(KvEntry::getBooleanValue).orElse(false)), MoreExecutors.directExecutor());
    }

    private List<RuleChain> findEdgeRuleChains(TenantId tenantId, EdgeId edgeId) {
        List<RuleChain> result = new ArrayList<>();
        PageDataIterable<RuleChain> ruleChains = new PageDataIterable<>(
                link -> ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, link), 1024);
        for (RuleChain ruleChain : ruleChains) {
            result.add(ruleChain);
        }
        return result;
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return edgeDao.countByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findEdgeById(tenantId, new EdgeId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

}
