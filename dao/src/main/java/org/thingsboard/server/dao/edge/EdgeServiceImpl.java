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
package org.thingsboard.server.dao.edge;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.EDGE_CACHE;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class EdgeServiceImpl extends AbstractEntityService implements EdgeService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";

    private static final int DEFAULT_PAGE_SIZE = 1000;

    @Autowired
    private EdgeDao edgeDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Override
    public Edge findEdgeById(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findById(tenantId, edgeId.getId());
    }

    @Override
    public EdgeInfo findEdgeInfoById(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeInfoById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findEdgeInfoById(tenantId, edgeId.getId());
    }

    @Override
    public ListenableFuture<Edge> findEdgeByIdAsync(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findByIdAsync(tenantId, edgeId.getId());
    }

    @Cacheable(cacheNames = EDGE_CACHE, key = "{#tenantId, #name}")
    @Override
    public Edge findEdgeByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findEdgeByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Optional<Edge> edgeOpt = edgeDao.findEdgeByTenantIdAndName(tenantId.getId(), name);
        return edgeOpt.orElse(null);
    }

    @Override
    public Optional<Edge> findEdgeByRoutingKey(TenantId tenantId, String routingKey) {
        log.trace("Executing findEdgeByRoutingKey [{}]", routingKey);
        Validator.validateString(routingKey, "Incorrect edge routingKey for search request.");
        return edgeDao.findByRoutingKey(tenantId.getId(), routingKey);
    }

    @CacheEvict(cacheNames = EDGE_CACHE, key = "{#edge.tenantId, #edge.name}")
    @Override
    public Edge saveEdge(Edge edge, boolean doValidate) {
        log.trace("Executing saveEdge [{}]", edge);
        if (doValidate) {
            edgeValidator.validate(edge, Edge::getTenantId);
        }
        try {
            return edgeDao.save(edge.getTenantId(), edge);
        } catch (Exception t) {
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
        edge.setCustomerId(customerId);
        return saveEdge(edge, true);
    }

    @Override
    public Edge unassignEdgeFromCustomer(TenantId tenantId, EdgeId edgeId) {
        log.trace("[{}] Executing unassignEdgeFromCustomer [{}]", tenantId, edgeId);
        Edge edge = findEdgeById(tenantId, edgeId);
        edge.setCustomerId(null);
        return saveEdge(edge, true);
    }

    @Override
    public void deleteEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing deleteEdge [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);

        Edge edge = edgeDao.findById(tenantId, edgeId.getId());

        deleteEntityRelations(tenantId, edgeId);

        removeEdgeFromCacheByName(edge.getTenantId(), edge.getName());

        edgeDao.removeById(tenantId, edgeId.getId());
    }

    private void removeEdgeFromCacheByName(TenantId tenantId, String name) {
        Cache cache = cacheManager.getCache(EDGE_CACHE);
        cache.evict(Arrays.asList(tenantId, name));
    }

    @Override
    public PageData<Edge> findEdgesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(TenantId tenantId, List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdAndIdsAsync, tenantId [{}], edgeIds [{}]", tenantId, edgeIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(edgeIds, "Incorrect edgeIds " + edgeIds);
        return edgeDao.findEdgesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(edgeIds));
    }

    @Override
    public void deleteEdgesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEdgesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantEdgesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], edgeIds [{}]", tenantId, customerId, edgeIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateIds(edgeIds, "Incorrect edgeIds " + edgeIds);
        return edgeDao.findEdgesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(edgeIds));
    }

    @Override
    public void unassignCustomerEdges(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerEdges, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerEdgeUnassigner.removeEntities(tenantId, customerId);
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

        edges = Futures.transform(edges, new Function<List<Edge>, List<Edge>>() {
            @Nullable
            @Override
            public List<Edge> apply(@Nullable List<Edge> edgeList) {
                return edgeList == null ? Collections.emptyList() : edgeList.stream().filter(edge -> query.getEdgeTypes().contains(edge.getType())).collect(Collectors.toList());
            }
        }, MoreExecutors.directExecutor());

        return edges;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findEdgeTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findEdgeTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
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
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findAutoAssignToEdgeRuleChainsByTenantId(tenantId, pageLink);
            if (pageData.getData().size() > 0) {
                for (RuleChain ruleChain : pageData.getData()) {
                    ruleChainService.assignRuleChainToEdge(tenantId, ruleChain.getId(), edgeId);
                }
            }
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndEntityId(tenantId.getId(), entityId.getId(), entityId.getEntityType(), pageLink);
    }

    private DataValidator<Edge> edgeValidator =
            new DataValidator<Edge>() {

                @Override
                protected void validateCreate(TenantId tenantId, Edge edge) {
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Edge edge) {
                    Edge old = edgeDao.findById(edge.getTenantId(), edge.getId().getId());
                    if (!old.getName().equals(edge.getName())) {
                        removeEdgeFromCacheByName(tenantId, old.getName());
                    }
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Edge edge) {
                    if (StringUtils.isEmpty(edge.getType())) {
                        throw new DataValidationException("Edge type should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getName())) {
                        throw new DataValidationException("Edge name should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getSecret())) {
                        throw new DataValidationException("Edge secret should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getRoutingKey())) {
                        throw new DataValidationException("Edge routing key should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getEdgeLicenseKey())) {
                        throw new DataValidationException("Edge license key should be specified!");
                    }
                    if (StringUtils.isEmpty(edge.getCloudEndpoint())) {
                        throw new DataValidationException("Cloud endpoint should be specified!");
                    }
                    if (edge.getTenantId() == null) {
                        throw new DataValidationException("Edge should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(edge.getTenantId(), edge.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Edge is referencing to non-existent tenant!");
                        }
                    }
                    if (edge.getCustomerId() == null) {
                        edge.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!edge.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(edge.getTenantId(), edge.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign edge to non-existent customer!");
                        }
                        if (!customer.getTenantId().getId().equals(edge.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign edge to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Edge> tenantEdgesRemover =
            new PaginatedRemover<TenantId, Edge>() {

                @Override
                protected PageData<Edge> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return edgeDao.findEdgesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Edge entity) {
                    deleteEdge(tenantId, new EdgeId(entity.getUuidId()));
                }
            };

    private PaginatedRemover<CustomerId, Edge> customerEdgeUnassigner = new PaginatedRemover<CustomerId, Edge>() {

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
    public PageData<EdgeId> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("[{}] Executing findRelatedEdgeIdsByEntityId [{}] [{}]", tenantId, entityId, pageLink);
        if (EntityType.TENANT.equals(entityId.getEntityType()) ||
                EntityType.CUSTOMER.equals(entityId.getEntityType()) ||
                EntityType.DEVICE_PROFILE.equals(entityId.getEntityType())) {
            if (EntityType.TENANT.equals(entityId.getEntityType()) ||
                    EntityType.DEVICE_PROFILE.equals(entityId.getEntityType())) {
                return convertToEdgeIds(findEdgesByTenantId(tenantId, pageLink));
            } else {
                return convertToEdgeIds(findEdgesByTenantIdAndCustomerId(tenantId, new CustomerId(entityId.getId()), pageLink));
            }
        } else {
            switch (entityId.getEntityType()) {
                case EDGE:
                    List<EdgeId> edgeIds = Collections.singletonList(new EdgeId(entityId.getId()));
                    return new PageData<>(edgeIds, 1, 1, false);
                case DEVICE:
                case ASSET:
                case ENTITY_VIEW:
                case DASHBOARD:
                case RULE_CHAIN:
                    return convertToEdgeIds(findEdgesByTenantIdAndEntityId(tenantId, entityId, pageLink));
                case USER:
                    User userById = userService.findUserById(tenantId, new UserId(entityId.getId()));
                    if (userById == null) {
                        return createEmptyEdgeIdPageData();
                    }
                    if (userById.getCustomerId() == null || userById.getCustomerId().isNullUid()) {
                        return convertToEdgeIds(findEdgesByTenantId(tenantId, pageLink));
                    } else {
                        return convertToEdgeIds(findEdgesByTenantIdAndCustomerId(tenantId, userById.getCustomerId(), pageLink));
                    }
                default:
                    log.warn("[{}] Unsupported entity type {}", tenantId, entityId.getEntityType());
                    return createEmptyEdgeIdPageData();
            }
        }
    }

    private PageData<EdgeId> createEmptyEdgeIdPageData() {
        return new PageData<>(new ArrayList<>(), 0, 0, false);
    }

    private PageData<EdgeId> convertToEdgeIds(PageData<Edge> pageData) {
        if (pageData == null) {
            return createEmptyEdgeIdPageData();
        }
        List<EdgeId> edgeIds = new ArrayList<>();
        if (pageData.getData() != null && !pageData.getData().isEmpty()) {
            edgeIds = pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        }
        return new PageData<>(edgeIds, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

    @Override
    public String findMissingToRelatedRuleChains(TenantId tenantId, EdgeId edgeId) {
        List<RuleChain> edgeRuleChains = findEdgeRuleChains(tenantId, edgeId);
        List<RuleChainId> edgeRuleChainIds = edgeRuleChains.stream().map(IdBased::getId).collect(Collectors.toList());
        ObjectNode result = JacksonUtil.newObjectNode();
        for (RuleChain edgeRuleChain : edgeRuleChains) {
            List<RuleChainConnectionInfo> connectionInfos =
                    ruleChainService.loadRuleChainMetaData(edgeRuleChain.getTenantId(), edgeRuleChain.getId()).getRuleChainConnections();
            if (connectionInfos != null && !connectionInfos.isEmpty()) {
                List<RuleChainId> connectedRuleChains =
                        connectionInfos.stream().map(RuleChainConnectionInfo::getTargetRuleChainId).collect(Collectors.toList());
                List<String> missingRuleChains = new ArrayList<>();
                for (RuleChainId connectedRuleChain : connectedRuleChains) {
                    if (!edgeRuleChainIds.contains(connectedRuleChain)) {
                        RuleChain ruleChainById = ruleChainService.findRuleChainById(tenantId, connectedRuleChain);
                        missingRuleChains.add(ruleChainById.getName());
                    }
                }
                if (!missingRuleChains.isEmpty()) {
                    ArrayNode array = JacksonUtil.createArrayNode();
                    for (String missingRuleChain : missingRuleChains) {
                        array.add(missingRuleChain);
                    }
                    result.set(edgeRuleChain.getName(), array);
                }
            }
        }
        return result.toString();
    }

    private List<RuleChain> findEdgeRuleChains(TenantId tenantId, EdgeId edgeId) {
        List<RuleChain> result = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                result.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return result;
    }
}
