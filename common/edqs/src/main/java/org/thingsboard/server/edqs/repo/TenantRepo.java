/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.edqs.repo;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.common.data.edqs.EdqsEvent;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.Entity;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.stats.EdqsStatsService;
import org.thingsboard.server.edqs.data.ApiUsageStateData;
import org.thingsboard.server.edqs.data.AssetData;
import org.thingsboard.server.edqs.data.CustomerData;
import org.thingsboard.server.edqs.data.DeviceData;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.EntityProfileData;
import org.thingsboard.server.edqs.data.GenericData;
import org.thingsboard.server.edqs.data.RelationsRepo;
import org.thingsboard.server.edqs.data.TenantData;
import org.thingsboard.server.edqs.query.EdqsDataQuery;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.query.processor.EntityQueryProcessor;
import org.thingsboard.server.edqs.query.processor.EntityQueryProcessorFactory;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.thingsboard.server.edqs.util.RepositoryUtils.SORT_ASC;
import static org.thingsboard.server.edqs.util.RepositoryUtils.SORT_DESC;
import static org.thingsboard.server.edqs.util.RepositoryUtils.resolveEntityType;

@Slf4j
public class TenantRepo {

    public static final Comparator<EntityData<?>> CREATED_TIME_COMPARATOR = Comparator.comparingLong(ed -> ed.getFields().getCreatedTime());
    public static final Comparator<EntityData<?>> CREATED_TIME_AND_ID_COMPARATOR = CREATED_TIME_COMPARATOR
            .thenComparing(EntityData::getId);
    public static final Comparator<EntityData<?>> CREATED_TIME_AND_ID_DESC_COMPARATOR = CREATED_TIME_AND_ID_COMPARATOR.reversed();

    private final ConcurrentMap<EntityType, Set<EntityData<?>>> entitySetByType = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityType, ConcurrentMap<UUID, EntityData<?>>> entityMapByType = new ConcurrentHashMap<>();
    private final ConcurrentMap<RelationTypeGroup, RelationsRepo> relations = new ConcurrentHashMap<>();

    private final Lock entityUpdateLock = new ReentrantLock();

    private final TenantId tenantId;
    private final EdqsStatsService edqsStatsService;

    public TenantRepo(TenantId tenantId, EdqsStatsService edqsStatsService) {
        this.tenantId = tenantId;
        this.edqsStatsService = edqsStatsService;
    }

    public void processEvent(EdqsEvent event) {
        EdqsObject edqsObject = event.getObject();
        log.trace("[{}] Processing event: {}", tenantId, event);
        if (event.getEventType() == EdqsEventType.UPDATED) {
            addOrUpdate(edqsObject);
        } else if (event.getEventType() == EdqsEventType.DELETED) {
            remove(edqsObject);
        }
    }

    public void addOrUpdate(EdqsObject object) {
        if (object instanceof EntityRelation relation) {
            addOrUpdateRelation(relation);
        } else if (object instanceof AttributeKv attributeKv) {
            addOrUpdateAttribute(attributeKv);
        } else if (object instanceof LatestTsKv latestTsKv) {
            addOrUpdateLatestKv(latestTsKv);
        } else if (object instanceof Entity entity) {
            addOrUpdateEntity(entity);
        }
    }

    public void remove(EdqsObject object) {
        if (object instanceof EntityRelation relation) {
            removeRelation(relation);
        } else if (object instanceof AttributeKv attributeKv) {
            removeAttribute(attributeKv);
        } else if (object instanceof LatestTsKv latestTsKv) {
            removeLatestKv(latestTsKv);
        } else if (object instanceof Entity entity) {
            removeEntity(entity);
        }
    }

    private void addOrUpdateRelation(EntityRelation entity) {
        entityUpdateLock.lock();
        try {
            if (RelationTypeGroup.COMMON.equals(entity.getTypeGroup())) {
                RelationsRepo repo = relations.computeIfAbsent(entity.getTypeGroup(), tg -> new RelationsRepo());
                EntityData<?> from = getOrCreate(entity.getFrom());
                EntityData<?> to = getOrCreate(entity.getTo());
                boolean added = repo.add(from, to, entity.getType());
                if (added) {
                    edqsStatsService.reportAdded(ObjectType.RELATION);
                }
            } else if (RelationTypeGroup.DASHBOARD.equals(entity.getTypeGroup())) {
                if (EntityRelation.CONTAINS_TYPE.equals(entity.getType()) && entity.getFrom().getEntityType() == EntityType.CUSTOMER) {
                    CustomerData customerData = (CustomerData) getOrCreate(entity.getFrom());
                    EntityData<?> dashboardData = getOrCreate(entity.getTo());
                    customerData.addOrUpdate(dashboardData);
                }
            }
        } finally {
            entityUpdateLock.unlock();
        }
    }

    private void removeRelation(EntityRelation entityRelation) {
        if (RelationTypeGroup.COMMON.equals(entityRelation.getTypeGroup())) {
            RelationsRepo relationsRepo = relations.get(entityRelation.getTypeGroup());
            if (relationsRepo != null) {
                boolean removed = relationsRepo.remove(entityRelation.getFrom().getId(), entityRelation.getTo().getId(), entityRelation.getType());
                if (removed) {
                    edqsStatsService.reportRemoved(ObjectType.RELATION);
                }
            }
        } else if (RelationTypeGroup.DASHBOARD.equals(entityRelation.getTypeGroup())) {
            if (EntityRelation.CONTAINS_TYPE.equals(entityRelation.getType()) && entityRelation.getFrom().getEntityType() == EntityType.CUSTOMER) {
                CustomerData customerData = (CustomerData) get(entityRelation.getFrom());
                if (customerData != null) {
                    customerData.remove(EntityType.DASHBOARD, entityRelation.getTo().getId());
                }
            }
        }
    }

    private void addOrUpdateEntity(Entity entity) {
        entityUpdateLock.lock();
        try {
            log.trace("[{}] addOrUpdateEntity: {}", tenantId, entity);
            EntityFields fields = entity.getFields();
            UUID entityId = fields.getId();
            EntityType entityType = entity.getType();

            EntityData entityData = getOrCreate(entityType, entityId);
            EntityFields oldFields = entityData.getFields();
            entityData.setFields(fields);
            if (oldFields == null) {
                getEntitySet(entityType).add(entityData);
            }

            UUID newCustomerId = fields.getCustomerId();
            UUID oldCustomerId = entityData.getCustomerId();
            entityData.setCustomerId(newCustomerId);
            if (entityIdMismatch(oldCustomerId, newCustomerId)) {
                if (oldCustomerId != null) {
                    CustomerData old = (CustomerData) get(EntityType.CUSTOMER, oldCustomerId);
                    if (old != null) {
                        old.remove(entityType, entityId);
                    }
                }
                if (newCustomerId != null) {
                    CustomerData newData = (CustomerData) getOrCreate(EntityType.CUSTOMER, newCustomerId);
                    newData.addOrUpdate(entityData);
                }
            }
        } finally {
            entityUpdateLock.unlock();
        }
    }

    public void removeEntity(Entity entity) {
        entityUpdateLock.lock();
        try {
            UUID entityId = entity.getFields().getId();
            EntityType entityType = entity.getType();
            EntityData<?> removed = getEntityMap(entityType).remove(entityId);
            if (removed != null) {
                if (removed.getFields() != null) {
                    getEntitySet(entityType).remove(removed);
                }
                edqsStatsService.reportRemoved(entity.type());

                UUID customerId = removed.getCustomerId();
                if (customerId != null) {
                    CustomerData customerData = (CustomerData) get(EntityType.CUSTOMER, customerId);
                    if (customerData != null) {
                        customerData.remove(entityType, entityId);
                    }
                }
            }
        } finally {
            entityUpdateLock.unlock();
        }
    }

    public void addOrUpdateAttribute(AttributeKv attributeKv) {
        var entityData = getOrCreate(attributeKv.getEntityId());
        if (entityData != null) {
            Integer keyId = KeyDictionary.get(attributeKv.getKey());
            boolean added = entityData.putAttr(keyId, attributeKv.getScope(), attributeKv.getDataPoint());
            if (added) {
                edqsStatsService.reportAdded(ObjectType.ATTRIBUTE_KV);
            }
        }
    }

    private void removeAttribute(AttributeKv attributeKv) {
        var entityData = get(attributeKv.getEntityId());
        if (entityData != null) {
            boolean removed = entityData.removeAttr(KeyDictionary.get(attributeKv.getKey()), attributeKv.getScope());
            if (removed) {
                edqsStatsService.reportRemoved(ObjectType.ATTRIBUTE_KV);
            }
        }
    }

    public void addOrUpdateLatestKv(LatestTsKv latestTsKv) {
        var entityData = getOrCreate(latestTsKv.getEntityId());
        if (entityData != null) {
            Integer keyId = KeyDictionary.get(latestTsKv.getKey());
            boolean added = entityData.putTs(keyId, latestTsKv.getDataPoint());
            if (added) {
                edqsStatsService.reportAdded(ObjectType.LATEST_TS_KV);
            }
        }
    }

    private void removeLatestKv(LatestTsKv latestTsKv) {
        var entityData = get(latestTsKv.getEntityId());
        if (entityData != null) {
            boolean removed = entityData.removeTs(KeyDictionary.get(latestTsKv.getKey()));
            if (removed) {
                edqsStatsService.reportRemoved(ObjectType.LATEST_TS_KV);
            }
        }
    }

    public ConcurrentMap<UUID, EntityData<?>> getEntityMap(EntityType entityType) {
        return entityMapByType.computeIfAbsent(entityType, et -> new ConcurrentHashMap<>());
    }

    //TODO: automatically remove entities that has nothing except the ID.
    private EntityData<?> getOrCreate(EntityId entityId) {
        return getOrCreate(entityId.getEntityType(), entityId.getId());
    }

    private EntityData<?> getOrCreate(EntityType entityType, UUID entityId) {
        return getEntityMap(entityType).computeIfAbsent(entityId, id -> {
            log.debug("[{}] Adding {} {}", tenantId, entityType, id);
            EntityData<?> entityData = constructEntityData(entityType, entityId);
            edqsStatsService.reportAdded(ObjectType.fromEntityType(entityType));
            return entityData;
        });
    }

    private EntityData<?> get(EntityId entityId) {
        return get(entityId.getEntityType(), entityId.getId());
    }

    private EntityData<?> get(EntityType entityType, UUID entityId) {
        return getEntityMap(entityType).get(entityId);
    }

    private EntityData<?> constructEntityData(EntityType entityType, UUID id) {
        EntityData<?> entityData = switch (entityType) {
            case DEVICE -> new DeviceData(id);
            case ASSET -> new AssetData(id);
            case DEVICE_PROFILE, ASSET_PROFILE -> new EntityProfileData(id, entityType);
            case CUSTOMER -> new CustomerData(id);
            case TENANT -> new TenantData(id);
            case API_USAGE_STATE -> new ApiUsageStateData(id);
            default -> new GenericData(entityType, id);
        };
        entityData.setRepo(this);
        return entityData;
    }

    private static boolean entityIdMismatch(UUID oldOrNull, UUID newOrNull) {
        if (oldOrNull == null) {
            return newOrNull != null;
        } else {
            return !oldOrNull.equals(newOrNull);
        }
    }

    public Set<EntityData<?>> getEntitySet(EntityType entityType) {
        return entitySetByType.computeIfAbsent(entityType, et -> new ConcurrentSkipListSet<>(CREATED_TIME_AND_ID_DESC_COMPARATOR));
    }

    public PageData<QueryResult> findEntityDataByQuery(CustomerId customerId, EntityDataQuery oldQuery, boolean ignorePermissionCheck) {
        EdqsDataQuery query = RepositoryUtils.toNewQuery(oldQuery);
        QueryContext ctx = buildContext(customerId, query.getEntityFilter(), ignorePermissionCheck);
        EntityQueryProcessor queryProcessor = EntityQueryProcessorFactory.create(this, ctx, query);
        return sortAndConvert(query, queryProcessor.processQuery(), ctx);
    }

    public long countEntitiesByQuery(CustomerId customerId, EntityCountQuery oldQuery, boolean ignorePermissionCheck) {
        EdqsQuery query = RepositoryUtils.toNewQuery(oldQuery);
        QueryContext ctx = buildContext(customerId, query.getEntityFilter(), ignorePermissionCheck);
        EntityQueryProcessor queryProcessor = EntityQueryProcessorFactory.create(this, ctx, query);
        return queryProcessor.count();
    }

    private PageData<QueryResult> sortAndConvert(EdqsDataQuery query, List<SortableEntityData> data, QueryContext ctx) {
        int totalSize = data.size();
        int totalPages = (int) Math.ceil((float) totalSize / query.getPageSize());
        int offset = query.getPage() * query.getPageSize();
        if (offset > totalSize) {
            return new PageData<>(Collections.emptyList(), totalPages, totalSize, false);
        } else {
            Comparator<SortableEntityData> comparator = EntityDataSortOrder.Direction.ASC.equals(query.getSortDirection()) ? SORT_ASC : SORT_DESC;
            long startTs = System.nanoTime();
//          IMPLEMENTATION THAT IS BASED ON PRIORITY_QUEUE
//            var requiredSize = Math.min(offset + query.getPageSize(), totalSize);
//            PriorityQueue<SortableEntityData> topN = new PriorityQueue<>(requiredSize, comparator.reversed());
//            for (SortableEntityData item : data) {
//                topN.add(item);
//                if (topN.size() > requiredSize) {
//                    topN.poll();
//                }
//            }
//            List<SortableEntityData> result = new ArrayList<>(topN);
//            Collections.reverse(result);
//            result = result.subList(offset, requiredSize);
//          IMPLEMENTATION THAT IS BASED ON TREE SET  (For offset + query.getPageSize() << totalSize)
            var requiredSize = Math.min(offset + query.getPageSize(), totalSize);
            TreeSet<SortableEntityData> topNSet = new TreeSet<>(comparator);
            for (SortableEntityData sp : data) {
                topNSet.add(sp);
                if (topNSet.size() > requiredSize) {
                    topNSet.pollLast();
                }
            }
            var result = topNSet.stream().skip(offset).limit(query.getPageSize()).collect(Collectors.toList());
//          IMPLEMENTATION THAT IS BASED ON TIM SORT (For offset + query.getPageSize() > totalSize / 2)
//            data.sort(comparator);
//            var result = data.subList(offset, endIndex);
            log.trace("EDQ Sorted in {}", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTs));
            return new PageData<>(toQueryResult(result, query, ctx), totalPages, totalSize, totalSize > requiredSize);
        }
    }

    private List<QueryResult> toQueryResult(List<SortableEntityData> data, EdqsDataQuery query, QueryContext ctx) {
        long ts = System.currentTimeMillis();
        List<QueryResult> results = new ArrayList<>(data.size());
        for (SortableEntityData entityData : data) {
            Map<EntityKeyType, Map<String, TsValue>> latest = new HashMap<>();
            for (var key : query.getEntityFields()) {
                DataPoint dp = entityData.getEntityData().getDataPoint(key, ctx);
                TsValue v = RepositoryUtils.toTsValue(ts, dp);
                latest.computeIfAbsent(EntityKeyType.ENTITY_FIELD, t -> new HashMap<>()).put(key.key(), v);
            }
            for (var key : query.getLatestValues()) {
                DataPoint dp = entityData.getEntityData().getDataPoint(key, ctx);
                TsValue v = RepositoryUtils.toTsValue(ts, dp);
                latest.computeIfAbsent(key.type(), t -> new HashMap<>()).put(KeyDictionary.get(key.keyId()), v);
            }

            results.add(new QueryResult(entityData.getEntityId(), latest));
        }
        return results;
    }

    private QueryContext buildContext(CustomerId customerId, EntityFilter filter, boolean ignorePermissionCheck) {
        return new QueryContext(tenantId, customerId, resolveEntityType(filter), ignorePermissionCheck);
    }

    public TenantId getTenantId() {
        return tenantId;
    }


    public RelationsRepo getRelations(RelationTypeGroup relationTypeGroup) {
        return relations.computeIfAbsent(relationTypeGroup, type -> new RelationsRepo());
    }

    public String getOwnerEntityName(EntityId entityId) {
        EntityType entityType = entityId.getEntityType();
        return switch (entityType) {
            case CUSTOMER, TENANT -> {
                EntityFields fields = get(entityId).getFields();
                yield fields != null ? fields.getName() : "";
            }
            default -> throw new RuntimeException("Unsupported entity type: " + entityType);
        };
    }

}
