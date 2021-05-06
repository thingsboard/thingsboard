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
package org.thingsboard.server.dao.relation;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static org.thingsboard.server.common.data.CacheConstants.RELATIONS_CACHE;

/**
 * Created by ashvayka on 28.04.17.
 */
@Service
@Slf4j
public class BaseRelationService implements RelationService {

    @Autowired
    private RelationDao relationDao;

    @Autowired
    private EntityService entityService;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public ListenableFuture<Boolean> checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing checkRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.checkRelation(tenantId, from, to, relationType, typeGroup);
    }

    @Cacheable(cacheNames = RELATIONS_CACHE, key = "{#from, #to, #relationType, #typeGroup}")
    @Override
    public EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        try {
            return getRelationAsync(tenantId, from, to, relationType, typeGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<EntityRelation> getRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing EntityRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.getRelation(tenantId, from, to, relationType, typeGroup);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.to, #relation.type, #relation.typeGroup}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.type, #relation.typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.to, #relation.typeGroup, 'TO'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.to, #relation.type, #relation.typeGroup, 'TO'}")
    })
    @Override
    public boolean saveRelation(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing saveRelation [{}]", relation);
        validate(relation);
        return relationDao.saveRelation(tenantId, relation);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.to, #relation.type, #relation.typeGroup}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.type, #relation.typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.to, #relation.typeGroup, 'TO'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.to, #relation.type, #relation.typeGroup, 'TO'}")
    })
    @Override
    public ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing saveRelationAsync [{}]", relation);
        validate(relation);
        return relationDao.saveRelationAsync(tenantId, relation);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.to, #relation.type, #relation.typeGroup}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.type, #relation.typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.to, #relation.typeGroup, 'TO'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.to, #relation.type, #relation.typeGroup, 'TO'}")
    })
    @Override
    public boolean deleteRelation(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing deleteRelation [{}]", relation);
        validate(relation);
        return relationDao.deleteRelation(tenantId, relation);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.to, #relation.type, #relation.typeGroup}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.type, #relation.typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.from, #relation.typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.to, #relation.typeGroup, 'TO'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#relation.to, #relation.type, #relation.typeGroup, 'TO'}")
    })
    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing deleteRelationAsync [{}]", relation);
        validate(relation);
        return relationDao.deleteRelationAsync(tenantId, relation);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#from, #to, #relationType, #typeGroup}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#from, #relationType, #typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#from, #typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#to, #typeGroup, 'TO'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#to, #relationType, #typeGroup, 'TO'}")
    })
    @Override
    public boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing deleteRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.deleteRelation(tenantId, from, to, relationType, typeGroup);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#from, #to, #relationType, #typeGroup}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#from, #relationType, #typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#from, #typeGroup, 'FROM'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#to, #typeGroup, 'TO'}"),
            @CacheEvict(cacheNames = RELATIONS_CACHE, key = "{#to, #relationType, #typeGroup, 'TO'}")
    })
    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing deleteRelationAsync [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.deleteRelationAsync(tenantId, from, to, relationType, typeGroup);
    }

    @Override
    public void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteEntityRelations [{}]", entityId);
        validate(entityId);
        final Cache cache = cacheManager.getCache(RELATIONS_CACHE);
        List<EntityRelation> inboundRelations = new ArrayList<>();
        for (RelationTypeGroup typeGroup : RelationTypeGroup.values()) {
            inboundRelations.addAll(relationDao.findAllByTo(tenantId, entityId, typeGroup));
        }

        List<EntityRelation> outboundRelations = new ArrayList<>();
        for (RelationTypeGroup typeGroup : RelationTypeGroup.values()) {
            outboundRelations.addAll(relationDao.findAllByFrom(tenantId, entityId, typeGroup));
        }

        for (EntityRelation relation : inboundRelations){
            delete(tenantId, cache, relation, true);
        }

        for (EntityRelation relation : outboundRelations){
            delete(tenantId, cache, relation, false);
        }

        relationDao.deleteOutboundRelations(tenantId, entityId);
    }

    @Override
    public ListenableFuture<Void> deleteEntityRelationsAsync(TenantId tenantId, EntityId entityId) {
        Cache cache = cacheManager.getCache(RELATIONS_CACHE);
        log.trace("Executing deleteEntityRelationsAsync [{}]", entityId);
        validate(entityId);
        List<ListenableFuture<List<EntityRelation>>> inboundRelationsList = new ArrayList<>();
        for (RelationTypeGroup typeGroup : RelationTypeGroup.values()) {
            inboundRelationsList.add(relationDao.findAllByToAsync(tenantId, entityId, typeGroup));
        }

        ListenableFuture<List<List<EntityRelation>>> inboundRelations = Futures.allAsList(inboundRelationsList);

        List<ListenableFuture<List<EntityRelation>>> outboundRelationsList = new ArrayList<>();
        for (RelationTypeGroup typeGroup : RelationTypeGroup.values()) {
            outboundRelationsList.add(relationDao.findAllByFromAsync(tenantId, entityId, typeGroup));
        }

        ListenableFuture<List<List<EntityRelation>>> outboundRelations = Futures.allAsList(outboundRelationsList);

        ListenableFuture<List<Boolean>> inboundDeletions = Futures.transformAsync(inboundRelations,
                relations -> {
                    List<ListenableFuture<Boolean>> results = deleteRelationGroupsAsync(tenantId, relations, cache, true);
                    return Futures.allAsList(results);
                }, MoreExecutors.directExecutor());

        ListenableFuture<List<Boolean>> outboundDeletions = Futures.transformAsync(outboundRelations,
                relations -> {
                    List<ListenableFuture<Boolean>> results = deleteRelationGroupsAsync(tenantId, relations, cache, false);
                    return Futures.allAsList(results);
                }, MoreExecutors.directExecutor());

        ListenableFuture<List<List<Boolean>>> deletionsFuture = Futures.allAsList(inboundDeletions, outboundDeletions);

        return Futures.transform(Futures.transformAsync(deletionsFuture,
                (deletions) -> relationDao.deleteOutboundRelationsAsync(tenantId, entityId),
                MoreExecutors.directExecutor()),
                result -> null, MoreExecutors.directExecutor());
    }

    private List<ListenableFuture<Boolean>> deleteRelationGroupsAsync(TenantId tenantId, List<List<EntityRelation>> relations, Cache cache, boolean deleteFromDb) {
        List<ListenableFuture<Boolean>> results = new ArrayList<>();
        for (List<EntityRelation> relationList : relations) {
            relationList.forEach(relation -> results.add(deleteAsync(tenantId, cache, relation, deleteFromDb)));
        }
        return results;
    }

    private ListenableFuture<Boolean> deleteAsync(TenantId tenantId, Cache cache, EntityRelation relation, boolean deleteFromDb) {
        cacheEviction(relation, cache);
        if (deleteFromDb) {
            return relationDao.deleteRelationAsync(tenantId, relation);
        } else {
            return Futures.immediateFuture(false);
        }
    }

    boolean delete(TenantId tenantId, Cache cache, EntityRelation relation, boolean deleteFromDb) {
        cacheEviction(relation, cache);
        if (deleteFromDb) {
            return relationDao.deleteRelation(tenantId, relation);
        } else {
            return false;
        }
    }

    private void cacheEviction(EntityRelation relation, Cache cache) {
        List<Object> fromToTypeAndTypeGroup = new ArrayList<>();
        fromToTypeAndTypeGroup.add(relation.getFrom());
        fromToTypeAndTypeGroup.add(relation.getTo());
        fromToTypeAndTypeGroup.add(relation.getType());
        fromToTypeAndTypeGroup.add(relation.getTypeGroup());
        cache.evict(fromToTypeAndTypeGroup);

        List<Object> fromTypeAndTypeGroup = new ArrayList<>();
        fromTypeAndTypeGroup.add(relation.getFrom());
        fromTypeAndTypeGroup.add(relation.getType());
        fromTypeAndTypeGroup.add(relation.getTypeGroup());
        fromTypeAndTypeGroup.add(EntitySearchDirection.FROM.name());
        cache.evict(fromTypeAndTypeGroup);

        List<Object> fromAndTypeGroup = new ArrayList<>();
        fromAndTypeGroup.add(relation.getFrom());
        fromAndTypeGroup.add(relation.getTypeGroup());
        fromAndTypeGroup.add(EntitySearchDirection.FROM.name());
        cache.evict(fromAndTypeGroup);

        List<Object> toAndTypeGroup = new ArrayList<>();
        toAndTypeGroup.add(relation.getTo());
        toAndTypeGroup.add(relation.getTypeGroup());
        toAndTypeGroup.add(EntitySearchDirection.TO.name());
        cache.evict(toAndTypeGroup);

        List<Object> toTypeAndTypeGroup = new ArrayList<>();
        toTypeAndTypeGroup.add(relation.getTo());
        toTypeAndTypeGroup.add(relation.getType());
        toTypeAndTypeGroup.add(relation.getTypeGroup());
        toTypeAndTypeGroup.add(EntitySearchDirection.TO.name());
        cache.evict(toTypeAndTypeGroup);
    }

    @Cacheable(cacheNames = RELATIONS_CACHE, key = "{#from, #typeGroup, 'FROM'}")
    @Override
    public List<EntityRelation> findByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        validate(from);
        validateTypeGroup(typeGroup);
        try {
            return relationDao.findAllByFromAsync(tenantId, from, typeGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByFromAsync(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        log.trace("Executing findByFrom [{}][{}]", from, typeGroup);
        validate(from);
        validateTypeGroup(typeGroup);

        List<Object> fromAndTypeGroup = new ArrayList<>();
        fromAndTypeGroup.add(from);
        fromAndTypeGroup.add(typeGroup);
        fromAndTypeGroup.add(EntitySearchDirection.FROM.name());

        Cache cache = cacheManager.getCache(RELATIONS_CACHE);
        @SuppressWarnings("unchecked")
        List<EntityRelation> fromCache = cache.get(fromAndTypeGroup, List.class);
        if (fromCache != null) {
            return Futures.immediateFuture(fromCache);
        } else {
            ListenableFuture<List<EntityRelation>> relationsFuture = relationDao.findAllByFromAsync(tenantId, from, typeGroup);
            Futures.addCallback(relationsFuture,
                    new FutureCallback<List<EntityRelation>>() {
                        @Override
                        public void onSuccess(@Nullable List<EntityRelation> result) {
                            cache.putIfAbsent(fromAndTypeGroup, result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                        }
                    }, MoreExecutors.directExecutor());
            return relationsFuture;
        }
    }

    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        log.trace("Executing findInfoByFrom [{}][{}]", from, typeGroup);
        validate(from);
        validateTypeGroup(typeGroup);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByFromAsync(tenantId, from, typeGroup);
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

    @Cacheable(cacheNames = RELATIONS_CACHE, key = "{#from, #relationType, #typeGroup, 'FROM'}")
    @Override
    public List<EntityRelation> findByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        try {
            return findByFromAndTypeAsync(tenantId, from, relationType, typeGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByFromAndTypeAsync(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByFromAndType [{}][{}][{}]", from, relationType, typeGroup);
        validate(from);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        return relationDao.findAllByFromAndType(tenantId, from, relationType, typeGroup);
    }

    @Cacheable(cacheNames = RELATIONS_CACHE, key = "{#to, #typeGroup, 'TO'}")
    @Override
    public List<EntityRelation> findByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        validate(to);
        validateTypeGroup(typeGroup);
        try {
            return relationDao.findAllByToAsync(tenantId, to, typeGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByToAsync(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        log.trace("Executing findByTo [{}][{}]", to, typeGroup);
        validate(to);
        validateTypeGroup(typeGroup);

        List<Object> toAndTypeGroup = new ArrayList<>();
        toAndTypeGroup.add(to);
        toAndTypeGroup.add(typeGroup);
        toAndTypeGroup.add(EntitySearchDirection.TO.name());

        Cache cache = cacheManager.getCache(RELATIONS_CACHE);
        @SuppressWarnings("unchecked")
        List<EntityRelation> fromCache = cache.get(toAndTypeGroup, List.class);
        if (fromCache != null) {
            return Futures.immediateFuture(fromCache);
        } else {
            ListenableFuture<List<EntityRelation>> relationsFuture = relationDao.findAllByToAsync(tenantId, to, typeGroup);
            Futures.addCallback(relationsFuture,
                    new FutureCallback<List<EntityRelation>>() {
                        @Override
                        public void onSuccess(@Nullable List<EntityRelation> result) {
                            cache.putIfAbsent(toAndTypeGroup, result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                        }
                    }, MoreExecutors.directExecutor());
            return relationsFuture;
        }
    }

    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        log.trace("Executing findInfoByTo [{}][{}]", to, typeGroup);
        validate(to);
        validateTypeGroup(typeGroup);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAsync(tenantId, to, typeGroup);
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
        ListenableFuture<String> entityName = entityService.fetchEntityNameAsync(tenantId, entityIdGetter.apply(relation));
        return Futures.transform(entityName, entityName1 -> {
            EntityRelationInfo entityRelationInfo1 = new EntityRelationInfo(relation);
            entityNameSetter.accept(entityRelationInfo1, entityName1);
            return entityRelationInfo1;
        }, MoreExecutors.directExecutor());
    }

    @Cacheable(cacheNames = RELATIONS_CACHE, key = "{#to, #relationType, #typeGroup, 'TO'}")
    @Override
    public List<EntityRelation> findByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        try {
            return findByToAndTypeAsync(tenantId, to, relationType, typeGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByToAndTypeAsync(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAndType [{}][{}][{}]", to, relationType, typeGroup);
        validate(to);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        return relationDao.findAllByToAndType(tenantId, to, relationType, typeGroup);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByQuery(TenantId tenantId, EntityRelationsQuery query) {
        //boolean fetchLastLevelOnly = true;
        log.trace("Executing findByQuery [{}]", query);
        RelationsSearchParameters params = query.getParameters();
        final List<RelationEntityTypeFilter> filters = query.getFilters();
        if (filters == null || filters.isEmpty()) {
            log.debug("Filters are not set [{}]", query);
        }

        int maxLvl = params.getMaxLevel() > 0 ? params.getMaxLevel() : Integer.MAX_VALUE;

        try {
            ListenableFuture<Set<EntityRelation>> relationSet = findRelationsRecursively(tenantId, params.getEntityId(), params.getDirection(), params.getRelationTypeGroup(), maxLvl, params.isFetchLastLevelOnly(), new ConcurrentHashMap<>());
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
        Cache cache = cacheManager.getCache(RELATIONS_CACHE);

        List<EntityRelation> relations = new ArrayList<>();
        for (RelationTypeGroup relationTypeGroup : RelationTypeGroup.values()) {
            relations.addAll(findByFrom(tenantId, entityId, relationTypeGroup));
            relations.addAll(findByTo(tenantId, entityId, relationTypeGroup));
        }

        for (EntityRelation relation : relations) {
            cacheEviction(relation, cache);
            deleteRelation(tenantId, relation);
        }
    }

    protected void validate(EntityRelation relation) {
        if (relation == null) {
            throw new DataValidationException("Relation type should be specified!");
        }
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

    private Function<List<Boolean>, Boolean> getListToBooleanFunction() {
        return new Function<List<Boolean>, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable List<Boolean> results) {
                for (Boolean result : results) {
                    if (result == null || !result) {
                        return false;
                    }
                }
                return true;
            }
        };
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

    private ListenableFuture<Set<EntityRelation>> findRelationsRecursively(final TenantId tenantId, final EntityId rootId, final EntitySearchDirection direction,
                                                                           RelationTypeGroup relationTypeGroup, int lvl, boolean fetchLastLevelOnly,
                                                                           final ConcurrentHashMap<EntityId, Boolean> uniqueMap) throws Exception {
        if (lvl == 0) {
            return Futures.immediateFuture(Collections.emptySet());
        }
        lvl--;
        //TODO: try to remove this blocking operation
        Set<EntityRelation> children = new HashSet<>(findRelations(tenantId, rootId, direction, relationTypeGroup).get());
        Set<EntityId> childrenIds = new HashSet<>();
        for (EntityRelation childRelation : children) {
            log.trace("Found Relation: {}", childRelation);
            EntityId childId;
            if (direction == EntitySearchDirection.FROM) {
                childId = childRelation.getTo();
            } else {
                childId = childRelation.getFrom();
            }
            if (uniqueMap.putIfAbsent(childId, Boolean.TRUE) == null) {
                log.trace("Adding Relation: {}", childId);
                if (childrenIds.add(childId)) {
                    log.trace("Added Relation: {}", childId);
                }
            }
        }
        List<ListenableFuture<Set<EntityRelation>>> futures = new ArrayList<>();
        for (EntityId entityId : childrenIds) {
            futures.add(findRelationsRecursively(tenantId, entityId, direction, relationTypeGroup, lvl, fetchLastLevelOnly, uniqueMap));
        }
        //TODO: try to remove this blocking operation
        List<Set<EntityRelation>> relations = Futures.successfulAsList(futures).get();
        if (fetchLastLevelOnly && lvl > 0) {
            children.clear();
        }
        relations.forEach(r -> r.forEach(children::add));
        return Futures.immediateFuture(children);
    }

    private ListenableFuture<List<EntityRelation>> findRelations(final TenantId tenantId, final EntityId rootId, final EntitySearchDirection direction, RelationTypeGroup relationTypeGroup) {
        ListenableFuture<List<EntityRelation>> relations;
        if (relationTypeGroup == null) {
            relationTypeGroup = RelationTypeGroup.COMMON;
        }
        if (direction == EntitySearchDirection.FROM) {
            relations = findByFromAsync(tenantId, rootId, relationTypeGroup);
        } else {
            relations = findByToAsync(tenantId, rootId, relationTypeGroup);
        }
        return relations;
    }
}
