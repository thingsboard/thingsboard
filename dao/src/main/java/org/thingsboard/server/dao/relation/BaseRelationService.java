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
package org.thingsboard.server.dao.relation;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationPathQuery;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.eventsourcing.RelationActionEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.sql.relation.JpaRelationQueryExecutorService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * Created by ashvayka on 28.04.17.
 */
@Service
@Slf4j
public class BaseRelationService implements RelationService {

    private final RelationDao relationDao;
    private final EntityService entityService;
    private final TbTransactionalCache<RelationCacheKey, RelationCacheValue> cache;
    private final ApplicationEventPublisher eventPublisher;
    private final JpaExecutorService executor;
    private final JpaRelationQueryExecutorService relationsExecutor;
    protected ScheduledExecutorService timeoutExecutorService;

    @Value("${sql.relations.query_timeout:20}")
    private Integer relationQueryTimeout;

    public BaseRelationService(RelationDao relationDao, @Lazy EntityService entityService,
                               TbTransactionalCache<RelationCacheKey, RelationCacheValue> cache,
                               ApplicationEventPublisher eventPublisher, JpaExecutorService executor,
                               JpaRelationQueryExecutorService relationsExecutor) {
        this.relationDao = relationDao;
        this.entityService = entityService;
        this.cache = cache;
        this.eventPublisher = eventPublisher;
        this.executor = executor;
        this.relationsExecutor = relationsExecutor;
    }

    @PostConstruct
    public void init() {
        timeoutExecutorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("relations-query-timeout");
    }

    @PreDestroy
    public void destroy() {
        if (timeoutExecutorService != null) {
            timeoutExecutorService.shutdownNow();
        }
    }

    @TransactionalEventListener(classes = EntityRelationEvent.class)
    public void handleEvictEvent(EntityRelationEvent event) {
        List<RelationCacheKey> keys = new ArrayList<>(5);
        keys.add(new RelationCacheKey(event.getFrom(), event.getTo(), event.getType(), event.getTypeGroup()));
        keys.add(new RelationCacheKey(event.getFrom(), null, event.getType(), event.getTypeGroup(), EntitySearchDirection.FROM));
        keys.add(new RelationCacheKey(event.getFrom(), null, null, event.getTypeGroup(), EntitySearchDirection.FROM));
        keys.add(new RelationCacheKey(null, event.getTo(), event.getType(), event.getTypeGroup(), EntitySearchDirection.TO));
        keys.add(new RelationCacheKey(null, event.getTo(), null, event.getTypeGroup(), EntitySearchDirection.TO));
        cache.evict(keys);
        log.debug("Processed evict event: {}", event);
    }

    @Override
    public ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing checkRelationAsync [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.checkRelationAsync(tenantId, from, to, relationType, typeGroup);
    }

    @Override
    public boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing checkRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.checkRelation(tenantId, from, to, relationType, typeGroup);
    }

    @Override
    public EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing EntityRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        RelationCacheKey cacheKey = new RelationCacheKey(from, to, relationType, typeGroup);
        return cache.getAndPutInTransaction(cacheKey,
                () -> {
                    log.trace("FETCH EntityRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
                    return relationDao.getRelation(tenantId, from, to, relationType, typeGroup);
                },
                RelationCacheValue::getRelation,
                relation -> RelationCacheValue.builder().relation(relation).build(), false);
    }

    @Override
    public EntityRelation saveRelation(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing saveRelation [{}]", relation);
        validate(relation);
        var result = relationDao.saveRelation(tenantId, relation);
        publishEvictEvent(EntityRelationEvent.from(result));
        eventPublisher.publishEvent(new RelationActionEvent(tenantId, result, ActionType.RELATION_ADD_OR_UPDATE));
        return result;
    }

    @Override
    public void saveRelations(TenantId tenantId, List<EntityRelation> relations) {
        log.trace("Executing saveRelations [{}]", relations);
        for (EntityRelation relation : relations) {
            validate(relation);
        }
        List<EntityRelation> savedRelations = new ArrayList<>(relations.size());
        for (List<EntityRelation> partition : Lists.partition(relations, 1024)) {
            savedRelations.addAll(relationDao.saveRelations(tenantId, partition));
        }
        for (EntityRelation relation : savedRelations) {
            publishEvictEvent(EntityRelationEvent.from(relation));
            eventPublisher.publishEvent(new RelationActionEvent(tenantId, relation, ActionType.RELATION_ADD_OR_UPDATE));
        }
    }

    @Override
    public ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing saveRelationAsync [{}]", relation);
        validate(relation);
        var future = relationDao.saveRelationAsync(tenantId, relation);
        return Futures.transform(future, savedRelation -> {
            if (savedRelation != null) {
                handleEvictEvent(EntityRelationEvent.from(savedRelation));
                eventPublisher.publishEvent(new RelationActionEvent(tenantId, savedRelation, ActionType.RELATION_ADD_OR_UPDATE));
            }
            return savedRelation != null;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing DeleteRelation [{}]", relation);
        validate(relation);
        var result = relationDao.deleteRelation(tenantId, relation);
        if (result != null) {
            publishEvictEvent(EntityRelationEvent.from(result));
            eventPublisher.publishEvent(new RelationActionEvent(tenantId, result, ActionType.RELATION_DELETED));
        }
        return result != null;
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing deleteRelationAsync [{}]", relation);
        validate(relation);
        var future = relationDao.deleteRelationAsync(tenantId, relation);
        return Futures.transform(future, deletedRelation -> {
            if (deletedRelation != null) {
                handleEvictEvent(EntityRelationEvent.from(deletedRelation));
                eventPublisher.publishEvent(new RelationActionEvent(tenantId, deletedRelation, ActionType.RELATION_DELETED));
            }
            return deletedRelation != null;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public EntityRelation deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing deleteRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        var result = relationDao.deleteRelation(tenantId, from, to, relationType, typeGroup);
        if (result != null) {
            publishEvictEvent(EntityRelationEvent.from(result));
            eventPublisher.publishEvent(new RelationActionEvent(tenantId, result, ActionType.RELATION_DELETED));
        }
        return result;
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing deleteRelationAsync [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        var future = relationDao.deleteRelationAsync(tenantId, from, to, relationType, typeGroup);
        return Futures.transform(future, deletedEvent -> {
            if (deletedEvent != null) {
                handleEvictEvent(EntityRelationEvent.from(deletedEvent));
                eventPublisher.publishEvent(new RelationActionEvent(tenantId, deletedEvent, ActionType.RELATION_DELETED));
            }
            return deletedEvent != null;
        }, MoreExecutors.directExecutor());
    }

    @Transactional
    @Override
    public void deleteEntityCommonRelations(TenantId tenantId, EntityId entityId) {
        deleteEntityRelations(tenantId, entityId, RelationTypeGroup.COMMON);
    }

    @Transactional
    @Override
    public void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        deleteEntityRelations(tenantId, entityId, null);
    }

    @Transactional
    public void deleteEntityRelations(TenantId tenantId, EntityId entityId, RelationTypeGroup relationTypeGroup) {
        log.trace("Executing deleteEntityRelations [{}]", entityId);
        validate(entityId);

        List<EntityRelation> inboundRelations;
        if (relationTypeGroup == null) {
            inboundRelations = relationDao.deleteInboundRelations(tenantId, entityId);
        } else {
            inboundRelations = relationDao.deleteInboundRelations(tenantId, entityId, relationTypeGroup);
        }

        for (EntityRelation relation : inboundRelations) {
            eventPublisher.publishEvent(EntityRelationEvent.from(relation));
            eventPublisher.publishEvent(new RelationActionEvent(tenantId, relation, ActionType.RELATION_DELETED));
        }

        List<EntityRelation> outboundRelations;
        if (relationTypeGroup == null) {
            outboundRelations = relationDao.deleteOutboundRelations(tenantId, entityId);
        } else {
            outboundRelations = relationDao.deleteOutboundRelations(tenantId, entityId, relationTypeGroup);
        }

        for (EntityRelation relation : outboundRelations) {
            eventPublisher.publishEvent(EntityRelationEvent.from(relation));
            eventPublisher.publishEvent(new RelationActionEvent(tenantId, relation, ActionType.RELATION_DELETED));
        }
    }

    @Override
    public List<EntityRelation> findByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        validate(from);
        validateTypeGroup(typeGroup);
        RelationCacheKey cacheKey = RelationCacheKey.builder().from(from).typeGroup(typeGroup).direction(EntitySearchDirection.FROM).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByFrom(tenantId, from, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByFromAsync(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        log.trace("Executing findByFrom [{}][{}]", from, typeGroup);
        validate(from);
        validateTypeGroup(typeGroup);

        var cacheValue = cache.get(RelationCacheKey.builder().from(from).typeGroup(typeGroup).direction(EntitySearchDirection.FROM).build());

        if (cacheValue != null && cacheValue.get() != null) {
            return Futures.immediateFuture(cacheValue.get().getRelations());
        } else {
            //Disabled cache put for the async requests due to limitations of the cache implementation (Redis lib does not support thread-safe transactions)
            return executor.submit(() -> findByFrom(tenantId, from, typeGroup));
        }
    }

    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        log.trace("Executing findInfoByFrom [{}][{}]", from, typeGroup);
        validate(from);
        validateTypeGroup(typeGroup);
        ListenableFuture<List<EntityRelation>> relations = executor.submit(() -> relationDao.findAllByFrom(tenantId, from, typeGroup));
        return Futures.transformAsync(relations,
                relations1 -> {
                    List<ListenableFuture<EntityRelationInfo>> futures = new ArrayList<>();
                    relations1.forEach(relation ->
                            futures.add(fetchRelationInfoAsync(tenantId, relation,
                                    EntityRelation::getTo,
                                    EntityRelationInfo::setToName))
                    );
                    return Futures.successfulAsList(futures);
                }, MoreExecutors.directExecutor());
    }

    @Override
    public List<EntityRelation> findByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        RelationCacheKey cacheKey = RelationCacheKey.builder().from(from).type(relationType).typeGroup(typeGroup).direction(EntitySearchDirection.FROM).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByFromAndType(tenantId, from, relationType, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByFromAndTypeAsync(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByFromAndType [{}][{}][{}]", from, relationType, typeGroup);
        validate(from);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        return executor.submit(() -> findByFromAndType(tenantId, from, relationType, typeGroup));
    }

    @Override
    public List<EntityRelation> findByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        validate(to);
        validateTypeGroup(typeGroup);
        RelationCacheKey cacheKey = RelationCacheKey.builder().to(to).typeGroup(typeGroup).direction(EntitySearchDirection.TO).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByTo(tenantId, to, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);

    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByToAsync(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAsync [{}][{}]", to, typeGroup);
        validate(to);
        validateTypeGroup(typeGroup);
        return executor.submit(() -> findByTo(tenantId, to, typeGroup));
    }

    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        log.trace("Executing findInfoByTo [{}][{}]", to, typeGroup);
        validate(to);
        validateTypeGroup(typeGroup);
        ListenableFuture<List<EntityRelation>> relations = findByToAsync(tenantId, to, typeGroup);
        return Futures.transformAsync(relations,
                relations1 -> {
                    List<ListenableFuture<EntityRelationInfo>> futures = new ArrayList<>();
                    relations1.forEach(relation ->
                            futures.add(fetchRelationInfoAsync(tenantId, relation,
                                    EntityRelation::getFrom,
                                    EntityRelationInfo::setFromName))
                    );
                    return Futures.successfulAsList(futures);
                }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<EntityRelationInfo> fetchRelationInfoAsync(TenantId tenantId, EntityRelation relation,
                                                                        Function<EntityRelation, EntityId> entityIdGetter,
                                                                        BiConsumer<EntityRelationInfo, String> entityNameSetter) {
        EntityRelationInfo relationInfo = new EntityRelationInfo(relation);
        entityNameSetter.accept(relationInfo,
                entityService.fetchEntityName(tenantId, entityIdGetter.apply(relation)).orElse("N/A"));
        return Futures.immediateFuture(relationInfo);
    }

    @Override
    public List<EntityRelation> findByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAndType [{}][{}][{}]", to, relationType, typeGroup);
        validate(to);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        RelationCacheKey cacheKey = RelationCacheKey.builder().to(to).type(relationType).typeGroup(typeGroup).direction(EntitySearchDirection.TO).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByToAndType(tenantId, to, relationType, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);

    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByToAndTypeAsync(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAndTypeAsync [{}][{}][{}]", to, relationType, typeGroup);
        validate(to);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        return executor.submit(() -> findByToAndType(tenantId, to, relationType, typeGroup));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByQuery(TenantId tenantId, EntityRelationsQuery query) {
        log.trace("Executing findByQuery [{}]", query);
        RelationsSearchParameters params = query.getParameters();
        final List<RelationEntityTypeFilter> filters = query.getFilters();
        if (filters == null || filters.isEmpty()) {
            log.debug("Filters are not set [{}]", query);
        }

        int maxLvl = params.getMaxLevel() > 0 ? params.getMaxLevel() : Integer.MAX_VALUE;

        try {
            ListenableFuture<Set<EntityRelation>> relationSet = findRelationsRecursively(tenantId, params.getEntityId(), params.getDirection(),
                    params.getRelationTypeGroup(), maxLvl, params.isFetchLastLevelOnly(), new ConcurrentHashMap<>());
            return Futures.transform(relationSet, input -> {
                List<EntityRelation> relations = new ArrayList<>();
                if (filters == null || filters.isEmpty()) {
                    relations.addAll(input);
                    return relations;
                }
                for (EntityRelation relation : input) {
                    if (matchFilters(filters, relation, params.getDirection())) {
                        relations.add(relation);
                    }
                }
                return relations;
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            log.warn("Failed to query relations: [{}]", query, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByQuery(TenantId tenantId, EntityRelationsQuery query) {
        log.trace("Executing findInfoByQuery [{}]", query);
        ListenableFuture<List<EntityRelation>> relations = findByQuery(tenantId, query);
        EntitySearchDirection direction = query.getParameters().getDirection();
        return Futures.transformAsync(relations,
                relations1 -> {
                    List<ListenableFuture<EntityRelationInfo>> futures = new ArrayList<>();
                    relations1.forEach(relation ->
                            futures.add(fetchRelationInfoAsync(tenantId, relation,
                                    relation2 -> direction == EntitySearchDirection.FROM ? relation2.getTo() : relation2.getFrom(),
                                    (EntityRelationInfo relationInfo, String entityName) -> {
                                        if (direction == EntitySearchDirection.FROM) {
                                            relationInfo.setToName(entityName);
                                        } else {
                                            relationInfo.setFromName(entityName);
                                        }
                                    }))
                    );
                    return Futures.successfulAsList(futures);
                }, MoreExecutors.directExecutor());
    }

    @Override
    public void removeRelations(TenantId tenantId, EntityId entityId) {
        log.trace("removeRelations {}", entityId);

        List<EntityRelation> relations = new ArrayList<>();
        for (RelationTypeGroup relationTypeGroup : RelationTypeGroup.values()) {
            relations.addAll(findByFrom(tenantId, entityId, relationTypeGroup));
            relations.addAll(findByTo(tenantId, entityId, relationTypeGroup));
        }

        for (EntityRelation relation : relations) {
            deleteRelation(tenantId, relation);
        }
    }

    @Override
    public List<EntityRelation> findRuleNodeToRuleChainRelations(TenantId tenantId, RuleChainType ruleChainType, int limit) {
        log.trace("Executing findRuleNodeToRuleChainRelations, tenantId [{}], ruleChainType {} and limit {}", tenantId, ruleChainType, limit);
        validateId(tenantId, id -> "Invalid tenant id: " + id);
        return relationDao.findRuleNodeToRuleChainRelations(ruleChainType, limit);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByRelationPathQueryAsync(TenantId tenantId, EntityRelationPathQuery relationPathQuery) {
        log.trace("Executing findByRelationPathQuery, tenantId [{}], relationPathQuery {}", tenantId, relationPathQuery);
        validateId(tenantId, id -> "Invalid tenant id: " + id);
        validate(relationPathQuery);
        if (relationPathQuery.levels().size() == 1) {
            RelationPathLevel relationPathLevel = relationPathQuery.levels().get(0);
            return switch (relationPathLevel.direction()) {
                case FROM -> findByFromAndTypeAsync(tenantId, relationPathQuery.rootEntityId(), relationPathLevel.relationType(), RelationTypeGroup.COMMON);
                case TO -> findByToAndTypeAsync(tenantId, relationPathQuery.rootEntityId(), relationPathLevel.relationType(), RelationTypeGroup.COMMON);
            };
        }
        return executor.submit(() -> relationDao.findByRelationPathQuery(tenantId, relationPathQuery));
    }

    @Override
    public List<EntityRelation> findByRelationPathQuery(TenantId tenantId, EntityRelationPathQuery relationPathQuery) {
        log.trace("Executing findByRelationPathQuery, tenantId [{}], relationPathQuery {}", tenantId, relationPathQuery);
        validateId(tenantId, id -> "Invalid tenant id: " + id);
        validate(relationPathQuery);
        if (relationPathQuery.levels().size() == 1) {
            RelationPathLevel relationPathLevel = relationPathQuery.levels().get(0);
            return switch (relationPathLevel.direction()) {
                case FROM -> findByFromAndType(tenantId, relationPathQuery.rootEntityId(), relationPathLevel.relationType(), RelationTypeGroup.COMMON);
                case TO -> findByToAndType(tenantId, relationPathQuery.rootEntityId(), relationPathLevel.relationType(), RelationTypeGroup.COMMON);
            };
        }
        return relationDao.findByRelationPathQuery(tenantId, relationPathQuery);
    }

    private void validate(EntityRelationPathQuery relationPathQuery) {
        validateId((UUIDBased) relationPathQuery.rootEntityId(), id -> "Invalid root entity id: " + id);
        List<RelationPathLevel> levels = relationPathQuery.levels();
        if (CollectionUtils.isEmpty(levels)) {
            throw new DataValidationException("Relation path levels should be specified!");
        }
        levels.forEach(RelationPathLevel::validate);
    }

    protected void validate(EntityRelation relation) {
        if (relation == null) {
            throw new DataValidationException("Relation type should be specified!");
        }
        ConstraintValidator.validateFields(relation);
        validate(relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
    }

    protected void validate(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        validateType(type);
        validateTypeGroup(typeGroup);
        if (from == null) {
            throw new DataValidationException("Relation should contain from entity!");
        }
        if (to == null) {
            throw new DataValidationException("Relation should contain to entity!");
        }
    }

    private void validateType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new DataValidationException("Relation type should be specified!");
        }
    }

    private void validateTypeGroup(RelationTypeGroup typeGroup) {
        if (typeGroup == null) {
            throw new DataValidationException("Relation type group should be specified!");
        }
    }

    protected void validate(EntityId entity) {
        if (entity == null) {
            throw new DataValidationException("Entity should be specified!");
        }
    }

    private boolean matchFilters(List<RelationEntityTypeFilter> filters, EntityRelation relation, EntitySearchDirection direction) {
        for (RelationEntityTypeFilter filter : filters) {
            if (match(filter, relation, direction)) {
                return true;
            }
        }
        return false;
    }

    private boolean match(RelationEntityTypeFilter filter, EntityRelation relation, EntitySearchDirection direction) {
        if (StringUtils.isEmpty(filter.getRelationType()) || filter.getRelationType().equals(relation.getType())) {
            if (filter.getEntityTypes() == null || filter.getEntityTypes().isEmpty()) {
                return true;
            } else {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                return filter.getEntityTypes().contains(entityId.getEntityType());
            }
        } else {
            return false;
        }
    }

    @RequiredArgsConstructor
    private static class RelationQueueCtx {
        final SettableFuture<Set<EntityRelation>> future = SettableFuture.create();
        final Set<EntityRelation> result = ConcurrentHashMap.newKeySet();
        final Queue<RelationTask> tasks = new ConcurrentLinkedQueue<>();

        final TenantId tenantId;
        final EntitySearchDirection direction;
        final RelationTypeGroup relationTypeGroup;
        final boolean fetchLastLevelOnly;
        final int maxLvl;
        final ConcurrentHashMap<EntityId, Boolean> uniqueMap;

    }

    @RequiredArgsConstructor
    private static class RelationTask {
        private final int currentLvl;
        private final EntityId root;
        private final List<EntityRelation> prevRelations;
    }

    private void processQueue(RelationQueueCtx ctx) {
        RelationTask task = ctx.tasks.poll();
        while (task != null) {
            List<EntityRelation> relations = findRelations(ctx.tenantId, task.root, ctx.direction, ctx.relationTypeGroup);
            Map<EntityId, List<EntityRelation>> newChildrenRelations = new HashMap<>();
            for (EntityRelation childRelation : relations) {
                log.trace("Found Relation: {}", childRelation);
                EntityId childId = ctx.direction == EntitySearchDirection.FROM ? childRelation.getTo() : childRelation.getFrom();
                if (ctx.uniqueMap.putIfAbsent(childId, Boolean.TRUE) == null) {
                    log.trace("Adding Relation: {}", childId);
                    newChildrenRelations.put(childId, new ArrayList<>());
                }
                if (ctx.fetchLastLevelOnly) {
                    var list = newChildrenRelations.get(childId);
                    if (list != null) {
                        list.add(childRelation);
                    }
                }
            }
            if (ctx.fetchLastLevelOnly) {
                if (relations.isEmpty()) {
                    ctx.result.addAll(task.prevRelations);
                } else if (task.currentLvl == ctx.maxLvl) {
                    ctx.result.addAll(relations);
                }
            } else {
                ctx.result.addAll(relations);
            }
            var finalTask = task;
            newChildrenRelations.forEach((child, childRelations) -> {
                var newLvl = finalTask.currentLvl + 1;
                if (newLvl <= ctx.maxLvl)
                    ctx.tasks.add(new RelationTask(newLvl, child, childRelations));
            });
            task = ctx.tasks.poll();
        }
        ctx.future.set(ctx.result);
    }

    private ListenableFuture<Set<EntityRelation>> findRelationsRecursively(final TenantId tenantId, final EntityId rootId, final EntitySearchDirection direction,
                                                                           RelationTypeGroup relationTypeGroup, int lvl, boolean fetchLastLevelOnly,
                                                                           final ConcurrentHashMap<EntityId, Boolean> uniqueMap) {
        if (lvl == 0) {
            return Futures.immediateFuture(Collections.emptySet());
        }
        var relationQueueCtx = new RelationQueueCtx(tenantId, direction, relationTypeGroup, fetchLastLevelOnly, lvl, uniqueMap);
        relationQueueCtx.tasks.add(new RelationTask(1, rootId, Collections.emptyList()));
        relationsExecutor.submit(() -> processQueue(relationQueueCtx));
        return Futures.withTimeout(relationQueueCtx.future, relationQueryTimeout, TimeUnit.SECONDS, timeoutExecutorService);
    }


    private List<EntityRelation> findRelations(final TenantId tenantId, final EntityId rootId, final EntitySearchDirection direction, RelationTypeGroup relationTypeGroup) {
        List<EntityRelation> relations;
        if (relationTypeGroup == null) {
            relationTypeGroup = RelationTypeGroup.COMMON;
        }
        if (direction == EntitySearchDirection.FROM) {
            relations = findByFrom(tenantId, rootId, relationTypeGroup);
        } else {
            relations = findByTo(tenantId, rootId, relationTypeGroup);
        }
        return relations;
    }

    private void publishEvictEvent(EntityRelationEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
        } else {
            handleEvictEvent(event);
        }
    }
}
