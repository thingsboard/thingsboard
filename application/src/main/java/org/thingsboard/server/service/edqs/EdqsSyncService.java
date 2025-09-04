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
package org.thingsboard.server.service.edqs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.Entity;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryEntry;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sql.relation.RelationRepository;
import org.thingsboard.server.dao.sqlts.latest.TsKvLatestRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.common.data.ObjectType.ATTRIBUTE_KV;
import static org.thingsboard.server.common.data.ObjectType.LATEST_TS_KV;
import static org.thingsboard.server.common.data.ObjectType.RELATION;
import static org.thingsboard.server.common.data.ObjectType.edqsTenantTypes;

@Slf4j
public abstract class EdqsSyncService {

    @Value("${queue.edqs.sync.entity_batch_size:10000}")
    private int entityBatchSize;
    @Value("${queue.edqs.sync.ts_batch_size:10000}")
    private int tsBatchSize;
    @Autowired
    private EntityDaoRegistry entityDaoRegistry;
    @Autowired
    private AttributesDao attributesDao;
    @Autowired
    private KeyDictionaryDao keyDictionaryDao;
    @Autowired
    private RelationRepository relationRepository;
    @Autowired
    private TsKvLatestRepository tsKvLatestRepository;
    @Autowired
    @Lazy
    private DefaultEdqsService edqsService;

    private final ConcurrentHashMap<UUID, EntityIdInfo> entityInfoMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> keys = new ConcurrentHashMap<>();

    private final Map<ObjectType, AtomicInteger> counters = new ConcurrentHashMap<>();

    public abstract boolean isSyncNeeded();

    public void sync() {
        log.info("Synchronizing data to EDQS");
        long startTs = System.currentTimeMillis();
        counters.clear();

        syncTenantEntities();
        syncRelations();
        loadKeyDictionary();
        syncAttributes();
        syncLatestTimeseries();

        counters.clear();
        log.info("Finished synchronizing data to EDQS in {} ms", (System.currentTimeMillis() - startTs));
    }

    private void process(TenantId tenantId, ObjectType type, EdqsObject object) {
        AtomicInteger counter = counters.computeIfAbsent(type, t -> new AtomicInteger());
        if (counter.incrementAndGet() % 10000 == 0) {
            log.info("Processed {} {} objects", counter.get(), type);
        }
        edqsService.processEvent(tenantId, type, EdqsEventType.UPDATED, object);
    }

    private void syncTenantEntities() {
        for (ObjectType type : edqsTenantTypes) {
            log.info("Synchronizing {} entities to EDQS", type);
            long ts = System.currentTimeMillis();
            EntityType entityType = type.toEntityType();
            Dao<?> dao = entityDaoRegistry.getDao(entityType);
            UUID lastId = UUID.fromString("00000000-0000-0000-0000-000000000000");
            while (true) {
                var batch = dao.findNextBatch(lastId, entityBatchSize);
                if (batch.isEmpty()) {
                    break;
                }
                for (EntityFields entityFields : batch) {
                    TenantId tenantId = TenantId.fromUUID(entityFields.getTenantId());
                    entityInfoMap.put(entityFields.getId(), new EntityIdInfo(entityType, tenantId));
                    process(tenantId, type, new Entity(entityType, entityFields));
                }
                EntityFields lastRecord = batch.get(batch.size() - 1);
                lastId = lastRecord.getId();
            }
            log.info("Finished synchronizing {} entities to EDQS in {} ms", type, (System.currentTimeMillis() - ts));
        }
    }

    private void syncRelations() {
        log.info("Synchronizing relations to EDQS");
        long ts = System.currentTimeMillis();
        UUID lastFromEntityId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String lastFromEntityType = "";
        String lastRelationTypeGroup = "";
        String lastRelationType = "";
        UUID lastToEntityId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String lastToEntityType = "";

        while (true) {
            List<RelationEntity> batch = relationRepository.findNextBatch(lastFromEntityId, lastFromEntityType, lastRelationTypeGroup,
                    lastRelationType, lastToEntityId, lastToEntityType, entityBatchSize);
            if (batch.isEmpty()) {
                break;
            }
            processRelationBatch(batch);

            RelationEntity lastRecord = batch.get(batch.size() - 1);
            lastFromEntityId = lastRecord.getFromId();
            lastFromEntityType = lastRecord.getFromType();
            lastRelationTypeGroup = lastRecord.getRelationTypeGroup();
            lastRelationType = lastRecord.getRelationType();
            lastToEntityId = lastRecord.getToId();
            lastToEntityType = lastRecord.getToType();
        }
        log.info("Finished synchronizing relations to EDQS in {} ms", (System.currentTimeMillis() - ts));
    }

    private void processRelationBatch(List<RelationEntity> relations) {
        for (RelationEntity relation : relations) {
            try {
                if (RelationTypeGroup.COMMON.name().equals(relation.getRelationTypeGroup())) {
                    EntityIdInfo entityIdInfo = entityInfoMap.get(relation.getFromId());
                    if (entityIdInfo != null) {
                        process(entityIdInfo.tenantId(), RELATION, relation.toData());
                    } else {
                        log.info("Relation from id not found: {} ", relation);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to sync relation: {}", relation, e);
            }
        }
    }

    private void loadKeyDictionary() {
        log.info("Loading key dictionary");
        long ts = System.currentTimeMillis();
        var keyDictionaryEntries = new PageDataIterable<>(keyDictionaryDao::findAll, 10000);
        for (KeyDictionaryEntry keyDictionaryEntry : keyDictionaryEntries) {
            keys.put(keyDictionaryEntry.getKeyId(), keyDictionaryEntry.getKey());
        }
        log.info("Finished loading key dictionary in {} ms", (System.currentTimeMillis() - ts));
    }

    private void syncAttributes() {
        log.info("Synchronizing attributes to EDQS");
        long ts = System.currentTimeMillis();

        UUID lastEntityId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        int lastAttributeType = Integer.MIN_VALUE;
        int lastAttributeKey = Integer.MIN_VALUE;

        while (true) {
            List<AttributeKvEntity> batch = attributesDao.findNextBatch(lastEntityId, lastAttributeType, lastAttributeKey, tsBatchSize);
            if (batch.isEmpty()) {
                break;
            }
            processAttributeBatch(batch);

            AttributeKvEntity lastRecord = batch.get(batch.size() - 1);
            lastEntityId = lastRecord.getId().getEntityId();
            lastAttributeType = lastRecord.getId().getAttributeType();
            lastAttributeKey = lastRecord.getId().getAttributeKey();
        }
        log.info("Finished synchronizing attributes to EDQS in {} ms", (System.currentTimeMillis() - ts));
    }

    private void processAttributeBatch(List<AttributeKvEntity> batch) {
        for (AttributeKvEntity attribute : batch) {
            try {
                attribute.setStrKey(getStrKeyOrFetchFromDb(attribute.getId().getAttributeKey()));
                UUID entityId = attribute.getId().getEntityId();
                EntityIdInfo entityIdInfo = entityInfoMap.get(entityId);
                if (entityIdInfo == null) {
                    log.debug("Skipping attribute with entity UUID {} as it is not found in entityInfoMap", entityId);
                    continue;
                }
                AttributeKv attributeKv = new AttributeKv(
                        EntityIdFactory.getByTypeAndUuid(entityIdInfo.entityType(), entityId),
                        AttributeScope.valueOf(attribute.getId().getAttributeType()),
                        attribute.toData(),
                        attribute.getVersion());
                process(entityIdInfo.tenantId(), ATTRIBUTE_KV, attributeKv);
            } catch (Exception e) {
                log.error("Failed to sync attribute: {}", attribute, e);
            }
        }
    }

    private void syncLatestTimeseries() {
        log.info("Synchronizing latest timeseries to EDQS");
        long ts = System.currentTimeMillis();
        UUID lastEntityId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        int lastKey = Integer.MIN_VALUE;

        while (true) {
            List<TsKvLatestEntity> batch = tsKvLatestRepository.findNextBatch(lastEntityId, lastKey, tsBatchSize);
            if (batch.isEmpty()) {
                break;
            }
            processTsKvLatestBatch(batch);

            TsKvLatestEntity lastRecord = batch.get(batch.size() - 1);
            lastEntityId = lastRecord.getEntityId();
            lastKey = lastRecord.getKey();
        }
        log.info("Finished synchronizing latest timeseries to EDQS in {} ms", (System.currentTimeMillis() - ts));
    }

    private void processTsKvLatestBatch(List<TsKvLatestEntity> tsKvLatestEntities) {
        for (TsKvLatestEntity tsKvLatestEntity : tsKvLatestEntities) {
            try {
                String strKey = getStrKeyOrFetchFromDb(tsKvLatestEntity.getKey());
                if (strKey == null) {
                    log.debug("Skipping latest timeseries with key {} as it is not found in key dictionary", tsKvLatestEntity.getKey());
                    continue;
                }
                tsKvLatestEntity.setStrKey(strKey);
                UUID entityUuid = tsKvLatestEntity.getEntityId();
                EntityIdInfo entityIdInfo = entityInfoMap.get(entityUuid);
                if (entityIdInfo != null) {
                    EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityIdInfo.entityType(), entityUuid);
                    LatestTsKv latestTsKv = new LatestTsKv(entityId, tsKvLatestEntity.toData(), tsKvLatestEntity.getVersion());
                    process(entityIdInfo.tenantId(), LATEST_TS_KV, latestTsKv);
                }
            } catch (Exception e) {
                log.error("Failed to sync latest timeseries: {}", tsKvLatestEntity, e);
            }
        }
    }

    private String getStrKeyOrFetchFromDb(int key) {
        String strKey = keys.get(key);
        if (strKey != null) {
            return strKey;
        } else {
            strKey = keyDictionaryDao.getKey(key);
            if (strKey != null) {
                keys.put(key, strKey);
            }
        }
        return strKey;
    }

    public record EntityIdInfo(EntityType entityType, TenantId tenantId) {
    }

}
