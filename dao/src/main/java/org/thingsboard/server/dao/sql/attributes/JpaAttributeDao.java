// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.attributes;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sql.citus.CitusKvWriteQueueSupport;
import org.thingsboard.server.dao.sql.citus.CitusQueuePartitioner;
import org.thingsboard.server.dao.sql.citus.CitusSettings;
import org.thingsboard.server.dao.sql.citus.routing.CitusShardRouter;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
@SqlDao
public class JpaAttributeDao extends JpaAbstractDaoListeningExecutorService implements AttributesDao {

    @Autowired
    ScheduledLogExecutorComponent logExecutor;

    @Autowired
    private AttributeKvRepository attributeKvRepository;

    @Autowired
    private AttributeKvInsertRepository attributeKvInsertRepository;

    @Autowired
    private StatsFactory statsFactory;

    @Autowired
    private KeyDictionaryDao keyDictionaryDao;

    @Autowired(required = false)
    private CitusQueuePartitioner citusQueuePartitioner;

    @Autowired(required = false)
    private CitusShardRouter kvShardRouter;

    @Autowired
    private CitusSettings citusSettings;

    private static final String REMOVE_WITH_VERSION_SEQ = "DELETE FROM attribute_kv WHERE entity_id = ? AND attribute_type = ? " +
            "AND attribute_key = ? RETURNING nextval('attribute_kv_version_seq')";
    private static final String REMOVE_WITH_VERSION_CITUS = "DELETE FROM attribute_kv WHERE entity_id = ? AND attribute_type = ? " +
            "AND attribute_key = ? RETURNING version";

    private static final String ATTRIBUTE_KV_PROJECTION =
            "SELECT entity_id, attribute_type, attribute_key, bool_v, str_v, long_v, dbl_v, json_v, last_update_ts, version FROM attribute_kv";
    private static final String FIND_ATTRIBUTE_KV_BY_KEY_QUERY =
            ATTRIBUTE_KV_PROJECTION + " WHERE entity_id = ? AND attribute_type = ? AND attribute_key = ?";
    private static final String FIND_ATTRIBUTE_KV_BY_KEYS_QUERY_PREFIX =
            ATTRIBUTE_KV_PROJECTION + " WHERE entity_id = ? AND attribute_type = ? AND attribute_key IN (";
    // Routed-read variant that resolves str_key via a join on the key_dictionary reference table (present on every
    // worker), so a whole-entity read is a single round trip instead of one coordinator getKey per returned row.
    // LEFT JOIN keeps row-for-row parity with the plain path: a row whose attribute_key has no dictionary entry
    // is still returned (with a null str_key) rather than silently dropped.
    private static final String FIND_ATTRIBUTE_KV_WITH_STR_KEY_BY_ENTITY_AND_TYPE_QUERY =
            "SELECT a.entity_id, a.attribute_type, a.attribute_key, a.bool_v, a.str_v, a.long_v, a.dbl_v, a.json_v, a.last_update_ts, a.version, kd.key AS str_key " +
                    "FROM attribute_kv a LEFT JOIN key_dictionary kd ON a.attribute_key = kd.key_id WHERE a.entity_id = ? AND a.attribute_type = ?";

    static final RowMapper<AttributeKvEntity> ATTRIBUTE_KV_ROW_MAPPER = (rs, rowNum) -> {
        AttributeKvEntity entity = new AttributeKvEntity();
        entity.setId(new AttributeKvCompositeKey(
                rs.getObject("entity_id", UUID.class),
                rs.getInt("attribute_type"),
                rs.getInt("attribute_key")));
        entity.setBooleanValue(rs.getObject("bool_v", Boolean.class));
        entity.setStrValue(rs.getString("str_v"));
        entity.setLongValue(rs.getObject("long_v", Long.class));
        entity.setDoubleValue(rs.getObject("dbl_v", Double.class));
        entity.setJsonValue(rs.getString("json_v"));
        entity.setLastUpdateTs(rs.getObject("last_update_ts", Long.class));
        entity.setVersion(rs.getObject("version", Long.class));
        return entity;
    };

    // Dedicated mapper for FIND_ATTRIBUTE_KV_WITH_STR_KEY_BY_ENTITY_AND_TYPE_QUERY: same columns as
    // ATTRIBUTE_KV_ROW_MAPPER plus the joined str_key, so callers do not resolve it per row afterwards.
    static final RowMapper<AttributeKvEntity> ATTRIBUTE_KV_WITH_STR_KEY_ROW_MAPPER = (rs, rowNum) -> {
        AttributeKvEntity entity = ATTRIBUTE_KV_ROW_MAPPER.mapRow(rs, rowNum);
        entity.setStrKey(rs.getString("str_key"));
        return entity;
    };

    private String removeWithVersionQuery;

    @Value("${sql.attributes.batch_size:1000}")
    private int batchSize;

    @Value("${sql.attributes.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.attributes.stats_print_interval_ms:1000}")
    private long statsPrintIntervalMs;

    @Value("${sql.attributes.batch_threads:4}")
    private int batchThreads;

    @Value("${sql.batch_sort:true}")
    private boolean batchSortEnabled;

    private TbSqlBlockingQueueWrapper<AttributeKvEntity, Long> queue;

    @PostConstruct
    private void init() {
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("Attributes")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("attributes")
                .batchSortEnabled(batchSortEnabled)
                .withResponse(true)
                .build();

        queue = CitusKvWriteQueueSupport.buildQueue(params, batchThreads, statsFactory,
                citusQueuePartitioner, entity -> entity.getId().getEntityId());

        Comparator<AttributeKvEntity> comparator =
                Comparator.comparing((AttributeKvEntity attributeKvEntity) -> attributeKvEntity.getId().getEntityId())
                        .thenComparing(attributeKvEntity -> attributeKvEntity.getId().getAttributeType())
                        .thenComparing(attributeKvEntity -> attributeKvEntity.getId().getAttributeKey());

        CitusKvWriteQueueSupport.initQueue(queue, logExecutor, citusQueuePartitioner, kvShardRouter,
                attributeKvInsertRepository, comparator, l -> l);

        initDeleteQuery();
    }

    // Selects the delete-with-version SQL for the active mode. Called from init() (not as its own @PostConstruct)
    // so its ordering relative to the rest of the init is defined rather than left to bean post-processing order.
    private void initDeleteQuery() {
        this.removeWithVersionQuery = citusSettings.isEnabled() ? REMOVE_WITH_VERSION_CITUS : REMOVE_WITH_VERSION_SEQ;
    }

    String getRemoveWithVersionQuery() {
        return removeWithVersionQuery;
    }

    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    @Override
    public Optional<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, String attributeKey) {
        if (CitusShardRouter.isRouting(kvShardRouter)) {
            int keyId = keyDictionaryDao.getOrSaveKeyId(attributeKey);
            List<AttributeKvEntity> rows = kvShardRouter.routedQuery(
                    entityId.getId(), FIND_ATTRIBUTE_KV_BY_KEY_QUERY, ATTRIBUTE_KV_ROW_MAPPER,
                    entityId.getId(), attributeScope.getId(), keyId);
            if (!rows.isEmpty()) {
                AttributeKvEntity attributeKvEntity = rows.get(0);
                attributeKvEntity.setStrKey(attributeKey);
                return Optional.ofNullable(DaoUtil.getData(attributeKvEntity));
            }
            return Optional.empty();
        }
        AttributeKvCompositeKey compositeKey =
                getAttributeKvCompositeKey(entityId, attributeScope.getId(), keyDictionaryDao.getOrSaveKeyId(attributeKey));
        Optional<AttributeKvEntity> attributeKvEntityOptional = attributeKvRepository.findById(compositeKey);
        if (attributeKvEntityOptional.isPresent()) {
            AttributeKvEntity attributeKvEntity = attributeKvEntityOptional.get();
            attributeKvEntity.setStrKey(attributeKey);
            return Optional.ofNullable(DaoUtil.getData(attributeKvEntity));
        }
        return Optional.ofNullable(DaoUtil.getData(attributeKvEntityOptional));
    }

    @Override
    public List<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, Collection<String> attributeKeys) {
        if (CitusShardRouter.isRouting(kvShardRouter)) {
            // Resolve every key id once here and keep the id->name mapping locally, so the returned rows are labelled
            // from this map instead of paying an uncached coordinator keyDictionaryDao.getKey per returned row.
            List<Integer> keyIds = new ArrayList<>(attributeKeys.size());
            Map<Integer, String> attributeKeyByKeyId = new HashMap<>();
            for (String attributeKey : attributeKeys) {
                int keyId = keyDictionaryDao.getOrSaveKeyId(attributeKey);
                keyIds.add(keyId);
                attributeKeyByKeyId.put(keyId, attributeKey);
            }
            if (keyIds.isEmpty()) {
                return Collections.emptyList();
            }
            String placeholders = keyIds.stream().map(k -> "?").collect(Collectors.joining(", "));
            String query = FIND_ATTRIBUTE_KV_BY_KEYS_QUERY_PREFIX + placeholders + ")";
            Object[] args = new Object[keyIds.size() + 2];
            args[0] = entityId.getId();
            args[1] = attributeScope.getId();
            for (int i = 0; i < keyIds.size(); i++) {
                args[i + 2] = keyIds.get(i);
            }
            List<AttributeKvEntity> attributes = kvShardRouter.routedQuery(entityId.getId(), query, ATTRIBUTE_KV_ROW_MAPPER, args);
            attributes.forEach(attributeKvEntity -> attributeKvEntity.setStrKey(attributeKeyByKeyId.get(attributeKvEntity.getId().getAttributeKey())));
            return DaoUtil.convertDataList(attributes);
        }
        List<AttributeKvCompositeKey> compositeKeys =
                attributeKeys
                        .stream()
                        .map(attributeKey ->
                                getAttributeKvCompositeKey(entityId, attributeScope.getId(), keyDictionaryDao.getOrSaveKeyId(attributeKey)))
                        .collect(Collectors.toList());
        List<AttributeKvEntity> attributes = attributeKvRepository.findAllById(compositeKeys);
        attributes.forEach(attributeKvEntity -> attributeKvEntity.setStrKey(keyDictionaryDao.getKey(attributeKvEntity.getId().getAttributeKey())));
        return DaoUtil.convertDataList(Lists.newArrayList(attributes));
    }

    @Override
    public List<AttributeKvEntry> findAll(TenantId tenantId, EntityId entityId, AttributeScope attributeScope) {
        if (CitusShardRouter.isRouting(kvShardRouter)) {
            // str_key resolved via the key_dictionary join in the mapper, so no per-row coordinator getKey afterwards.
            List<AttributeKvEntity> attributes = kvShardRouter.routedQuery(
                    entityId.getId(), FIND_ATTRIBUTE_KV_WITH_STR_KEY_BY_ENTITY_AND_TYPE_QUERY, ATTRIBUTE_KV_WITH_STR_KEY_ROW_MAPPER,
                    entityId.getId(), attributeScope.getId());
            return DaoUtil.convertDataList(attributes);
        }
        List<AttributeKvEntity> attributes = attributeKvRepository.findAllByEntityIdAndAttributeType(
                entityId.getId(),
                attributeScope.getId());
        attributes.forEach(attributeKvEntity -> attributeKvEntity.setStrKey(keyDictionaryDao.getKey(attributeKvEntity.getId().getAttributeKey())));
        return DaoUtil.convertDataList(Lists.newArrayList(attributes));
    }

    @Override
    public List<AttributeKvEntity> findNextBatch(UUID entityId, int attributeType, int attributeKey, int batchSize) {
        return attributeKvRepository.findNextBatch(entityId, attributeType, attributeKey, batchSize);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        if (deviceProfileId != null) {
            return attributeKvRepository.findAllKeysByDeviceProfileId(tenantId.getId(), deviceProfileId.getId())
                    .stream().map(id -> keyDictionaryDao.getKey(id)).collect(Collectors.toList());
        } else {
            return attributeKvRepository.findAllKeysByTenantId(tenantId.getId())
                    .stream().map(id -> keyDictionaryDao.getKey(id)).collect(Collectors.toList());
        }
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        return attributeKvRepository
                .findAllKeysByEntityIds(entityIds.stream().map(EntityId::getId).collect(Collectors.toList()))
                .stream().map(id -> keyDictionaryDao.getKey(id)).collect(Collectors.toList());
    }

    @Override
    public List<String> findAllKeysByEntityIdsAndScope(TenantId tenantId, List<EntityId> entityIds, AttributeScope scope) {
        return attributeKvRepository
                .findAllKeysByEntityIdsAndAttributeType(entityIds.stream().map(EntityId::getId).toList(), scope.getId())
                .stream()
                .map(keyDictionaryDao::getKey)
                .toList();
    }

    @Override
    public ListenableFuture<List<String>> findAllKeysByEntityIdsAndScopeAsync(TenantId tenantId, List<EntityId> entityIds, AttributeScope scope) {
        return service.submit(() -> findAllKeysByEntityIdsAndScope(tenantId, entityIds, scope));
    }

    @Override
    public List<AttributeKvEntry> findLatestByEntityIdsAndScope(TenantId tenantId, List<EntityId> entityIds, AttributeScope scope) {
        if (CollectionUtils.isEmpty(entityIds)) {
            return Collections.emptyList();
        }
        var uniqueIds = entityIds.stream().map(EntityId::getId).distinct().toList();
        return attributeKvRepository.findLatestByEntityIdsAndAttributeType(uniqueIds, scope.getId())
                .stream()
                .map(AttributeKvRepository.AttributeKvProjection::toAttributeKvEntry)
                .toList();
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findLatestByEntityIdsAndScopeAsync(TenantId tenantId, List<EntityId> entityIds, AttributeScope scope) {
        return service.submit(() -> findLatestByEntityIdsAndScope(tenantId, entityIds, scope));
    }

    @Override
    public ListenableFuture<Long> save(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, AttributeKvEntry attribute) {
        AttributeKvEntity entity = new AttributeKvEntity();
        entity.setId(new AttributeKvCompositeKey(entityId.getId(), attributeScope.getId(), keyDictionaryDao.getOrSaveKeyId(attribute.getKey())));
        entity.setLastUpdateTs(attribute.getLastUpdateTs());
        entity.setStrValue(attribute.getStrValue().orElse(null));
        entity.setDoubleValue(attribute.getDoubleValue().orElse(null));
        entity.setLongValue(attribute.getLongValue().orElse(null));
        entity.setBooleanValue(attribute.getBooleanValue().orElse(null));
        entity.setJsonValue(attribute.getJsonValue().orElse(null));
        return addToQueue(entity);
    }

    private ListenableFuture<Long> addToQueue(AttributeKvEntity entity) {
        return queue.add(entity);
    }

    @Override
    public List<ListenableFuture<String>> removeAll(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, List<String> keys) {
        List<ListenableFuture<String>> futuresList = new ArrayList<>(keys.size());
        for (String key : keys) {
            futuresList.add(service.submit(() -> {
                attributeKvRepository.delete(entityId.getId(), attributeScope.getId(), keyDictionaryDao.getOrSaveKeyId(key));
                return key;
            }));
        }
        return futuresList;
    }

    @Override
    public List<ListenableFuture<TbPair<String, Long>>> removeAllWithVersions(TenantId tenantId, EntityId entityId, AttributeScope attributeScope, List<String> keys) {
        List<ListenableFuture<TbPair<String, Long>>> futuresList = new ArrayList<>(keys.size());
        for (String key : keys) {
            futuresList.add(service.submit(() -> {
                Long version;
                if (CitusShardRouter.isRouting(kvShardRouter)) {
                    version = kvShardRouter.routedQuery(entityId.getId(), removeWithVersionQuery,
                            rs -> rs.next() ? rs.getObject(1, Long.class) : null,
                            entityId.getId(), attributeScope.getId(), keyDictionaryDao.getOrSaveKeyId(key));
                } else {
                    version = transactionTemplate.execute(status -> jdbcTemplate.query(removeWithVersionQuery,
                            rs -> rs.next() ? rs.getObject(1, Long.class) : null, entityId.getId(), attributeScope.getId(), keyDictionaryDao.getOrSaveKeyId(key)));
                }
                return TbPair.of(key, version);
            }));
        }
        return futuresList;
    }

    @Transactional
    @Override
    public List<Pair<AttributeScope, String>> removeAllByEntityId(TenantId tenantId, EntityId entityId) {
        return jdbcTemplate.queryForList("DELETE FROM attribute_kv WHERE entity_id = ? " +
                        "RETURNING attribute_type, attribute_key", entityId.getId()).stream()
                .map(row -> Pair.of(AttributeScope.valueOf((Integer) row.get(ModelConstants.ATTRIBUTE_TYPE_COLUMN)),
                        keyDictionaryDao.getKey((Integer) row.get(ModelConstants.ATTRIBUTE_KEY_COLUMN))))
                .collect(Collectors.toList());
    }

    private AttributeKvCompositeKey getAttributeKvCompositeKey(EntityId entityId, Integer attributeType, Integer attributeKey) {
        return new AttributeKvCompositeKey(
                entityId.getId(),
                attributeType,
                attributeKey);
    }
}
