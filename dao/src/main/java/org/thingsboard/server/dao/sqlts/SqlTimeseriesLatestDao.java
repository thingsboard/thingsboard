// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sqlts;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestCompositeKey;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sql.TbSqlQueueElement;
import org.thingsboard.server.dao.sql.citus.CitusKvWriteQueueSupport;
import org.thingsboard.server.dao.sql.citus.CitusQueuePartitioner;
import org.thingsboard.server.dao.sql.citus.CitusSettings;
import org.thingsboard.server.dao.sql.citus.routing.CitusShardRouter;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.thingsboard.server.dao.sqlts.latest.SearchTsKvLatestRepository;
import org.thingsboard.server.dao.sqlts.latest.TsKvLatestRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@SqlTsLatestAnyDao
public class SqlTimeseriesLatestDao extends BaseAbstractSqlTimeseriesDao implements TimeseriesLatestDao {

    private static final String DESC_ORDER = "DESC";

    @Autowired
    private TsKvLatestRepository tsKvLatestRepository;

    @Autowired
    protected AggregationTimeseriesDao aggregationTimeseriesDao;

    @Autowired
    private SearchTsKvLatestRepository searchTsKvLatestRepository;

    @Autowired
    private InsertLatestTsRepository insertLatestTsRepository;

    private TbSqlBlockingQueueWrapper<TsKvLatestEntity, Long> tsLatestQueue;

    @Value("${sql.ts_latest.batch_size:1000}")
    private int tsLatestBatchSize;

    @Value("${sql.ts_latest.batch_max_delay:100}")
    private long tsLatestMaxDelay;

    @Value("${sql.ts_latest.stats_print_interval_ms:1000}")
    private long tsLatestStatsPrintIntervalMs;

    @Value("${sql.ts_latest.batch_threads:4}")
    private int tsLatestBatchThreads;

    @Value("${sql.batch_sort:true}")
    protected boolean batchSortEnabled;

    @Autowired
    protected ScheduledLogExecutorComponent logExecutor;

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

    private static final String REMOVE_LATEST_WITH_VERSION_SEQ = "DELETE FROM ts_kv_latest WHERE entity_id = ? " +
            "AND key = ? RETURNING nextval('ts_kv_latest_version_seq')";
    private static final String REMOVE_LATEST_WITH_VERSION_CITUS = "DELETE FROM ts_kv_latest WHERE entity_id = ? " +
            "AND key = ? RETURNING version";

    private static final String FIND_TS_KV_LATEST_BY_KEY_QUERY =
            "SELECT entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v, version FROM ts_kv_latest WHERE entity_id = ? AND key = ?";
    // INNER JOIN is deliberate: the plain-path query this routed read mirrors (SearchTsKvLatestRepository.FIND_ALL_BY_ENTITY_ID_QUERY)
    // also INNER JOINs key_dictionary, so INNER JOIN here is what preserves plain-vs-routed parity. Switching to LEFT JOIN
    // would silently diverge routed reads from plain-mode behavior. (Contrast the attribute sibling
    // JpaAttributeDao.FIND_ATTRIBUTE_KV_WITH_STR_KEY_BY_ENTITY_AND_TYPE_QUERY, whose plain path returns rows regardless of a
    // dictionary entry and therefore uses LEFT JOIN.)
    private static final String FIND_ALL_TS_KV_LATEST_BY_ENTITY_QUERY =
            "SELECT t.entity_id, t.key, t.ts, t.bool_v, t.str_v, t.long_v, t.dbl_v, t.json_v, t.version, kd.key AS str_key " +
                    "FROM ts_kv_latest t INNER JOIN key_dictionary kd ON t.key = kd.key_id WHERE t.entity_id = ?";

    static final RowMapper<TsKvLatestEntity> TS_KV_LATEST_ROW_MAPPER = (rs, rowNum) -> mapTsKvLatest(rs, null);
    static final RowMapper<TsKvLatestEntity> TS_KV_LATEST_WITH_STR_KEY_ROW_MAPPER = (rs, rowNum) -> mapTsKvLatest(rs, rs.getString("str_key"));

    private static TsKvLatestEntity mapTsKvLatest(ResultSet rs, String strKey) throws SQLException {
        return new TsKvLatestEntity(
                rs.getObject("entity_id", UUID.class),
                rs.getInt("key"),
                strKey,
                rs.getString("str_v"),
                rs.getObject("bool_v", Boolean.class),
                rs.getObject("long_v", Long.class),
                rs.getObject("dbl_v", Double.class),
                rs.getString("json_v"),
                rs.getObject("ts", Long.class),
                rs.getObject("version", Long.class));
    }

    private String removeLatestWithVersionQuery;

    String getRemoveLatestWithVersionQuery() {
        return removeLatestWithVersionQuery;
    }

    // Selects the delete-with-version SQL for the active mode. Called from init() (not as its own @PostConstruct)
    // so its ordering relative to the rest of the init is defined rather than left to bean post-processing order.
    private void initDeleteQuery() {
        this.removeLatestWithVersionQuery = citusSettings.isEnabled() ? REMOVE_LATEST_WITH_VERSION_CITUS : REMOVE_LATEST_WITH_VERSION_SEQ;
    }

    @PostConstruct
    protected void init() {
        initDeleteQuery();

        TbSqlBlockingQueueParams tsLatestParams = TbSqlBlockingQueueParams.builder()
                .logName("TS Latest")
                .batchSize(tsLatestBatchSize)
                .maxDelay(tsLatestMaxDelay)
                .statsPrintIntervalMs(tsLatestStatsPrintIntervalMs)
                .statsNamePrefix("ts.latest")
                .batchSortEnabled(batchSortEnabled)
                .withResponse(true)
                .build();

        tsLatestQueue = CitusKvWriteQueueSupport.buildQueue(tsLatestParams, tsLatestBatchThreads,
                statsFactory, citusQueuePartitioner, TsKvLatestEntity::getEntityId);

        Comparator<TsKvLatestEntity> comparator =
                Comparator.comparing((Function<TsKvLatestEntity, UUID>) AbstractTsKvEntity::getEntityId)
                        .thenComparingInt(AbstractTsKvEntity::getKey);
        Function<List<TbSqlQueueElement<TsKvLatestEntity, Long>>, List<TbSqlQueueElement<TsKvLatestEntity, Long>>> trueLatestFilter =
                v -> {
                    Map<TsKey, TbSqlQueueElement<TsKvLatestEntity, Long>> trueLatest = new HashMap<>();
                    v.forEach(element -> {
                        var entity = element.getEntity();
                        TsKey key = new TsKey(entity.getEntityId(), entity.getKey());
                        trueLatest.merge(key, element, (oldElement, newElement) -> oldElement.getEntity().getTs() <= newElement.getEntity().getTs() ? newElement : oldElement);
                    });
                    return new ArrayList<>(trueLatest.values());
                };

        CitusKvWriteQueueSupport.initQueue(tsLatestQueue, logExecutor, citusQueuePartitioner, kvShardRouter,
                insertLatestTsRepository, comparator, trueLatestFilter);
    }

    @PreDestroy
    protected void destroy() {
        if (tsLatestQueue != null) {
            tsLatestQueue.destroy();
        }
    }

    @Override
    public ListenableFuture<Long> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return getSaveLatestFuture(entityId, tsKvEntry);
    }

    @Override
    public ListenableFuture<TsKvLatestRemovingResult> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return getRemoveLatestFuture(tenantId, entityId, query);
    }

    @Override
    public ListenableFuture<Optional<TsKvEntry>> findLatestOpt(TenantId tenantId, EntityId entityId, String key) {
        return service.submit(() -> Optional.ofNullable(doFindLatestSync(entityId, key)));
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        log.trace("findLatest [{}][{}][{}]", tenantId, entityId, key);
        return service.submit(() -> wrapNullTsKvEntry(key, doFindLatestSync(entityId, key)));
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return getFindAllLatestFuture(entityId);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        if (deviceProfileId != null) {
            return tsKvLatestRepository.getKeysByDeviceProfileId(tenantId.getId(), deviceProfileId.getId());
        } else {
            return tsKvLatestRepository.getKeysByTenantId(tenantId.getId());
        }
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        return tsKvLatestRepository.findAllKeysByEntityIds(entityIds.stream().map(EntityId::getId).toList());
    }

    @Override
    public ListenableFuture<List<String>> findAllKeysByEntityIdsAsync(TenantId tenantId, List<EntityId> entityIds) {
        return service.submit(() -> findAllKeysByEntityIds(tenantId, entityIds));
    }

    @Override
    public List<TsKvEntry> findLatestByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        if (CollectionUtils.isEmpty(entityIds)) {
            return Collections.emptyList();
        }
        return DaoUtil.convertDataList(
                searchTsKvLatestRepository.findLatestByEntityIds(entityIds.stream().map(EntityId::getId).toList())
        );
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findLatestByEntityIdsAsync(TenantId tenantId, List<EntityId> entityIds) {
        return service.submit(() -> findLatestByEntityIds(tenantId, entityIds));
    }

    private ListenableFuture<TsKvLatestRemovingResult> getNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query, Long version) {
        ListenableFuture<List<TsKvEntry>> future = findNewLatestEntryFuture(tenantId, entityId, query);
        return Futures.transformAsync(future, entryList -> {
            if (entryList.size() == 1) {
                TsKvEntry entry = entryList.get(0);
                return Futures.transform(getSaveLatestFuture(entityId, entry), v -> new TsKvLatestRemovingResult(entry, v), MoreExecutors.directExecutor());
            } else {
                log.trace("Could not find new latest value for [{}], key - {}", entityId, query.getKey());
            }
            return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), true, version));
        }, service);
    }

    private ListenableFuture<List<TsKvEntry>> findNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                Aggregation.NONE, DESC_ORDER);
        return Futures.transform(aggregationTimeseriesDao.findAllAsync(tenantId, entityId, findNewLatestQuery),
                ReadTsKvQueryResult::getData, MoreExecutors.directExecutor());
    }

    protected TsKvEntry doFindLatestSync(EntityId entityId, String key) {
        if (CitusShardRouter.isRouting(kvShardRouter)) {
            int keyId = keyDictionaryDao.getOrSaveKeyId(key);
            List<TsKvLatestEntity> rows = kvShardRouter.routedQuery(
                    entityId.getId(), FIND_TS_KV_LATEST_BY_KEY_QUERY, TS_KV_LATEST_ROW_MAPPER,
                    entityId.getId(), keyId);
            if (!rows.isEmpty()) {
                TsKvLatestEntity tsKvLatestEntity = rows.get(0);
                tsKvLatestEntity.setStrKey(key);
                return DaoUtil.getData(tsKvLatestEntity);
            }
            return null;
        }
        TsKvLatestCompositeKey compositeKey =
                new TsKvLatestCompositeKey(
                        entityId.getId(),
                        keyDictionaryDao.getOrSaveKeyId(key));
        Optional<TsKvLatestEntity> entry = tsKvLatestRepository.findById(compositeKey);
        if (entry.isPresent()) {
            TsKvLatestEntity tsKvLatestEntity = entry.get();
            tsKvLatestEntity.setStrKey(key);
            return DaoUtil.getData(tsKvLatestEntity);
        } else {
            return null;
        }
    }

    protected ListenableFuture<TsKvLatestRemovingResult> getRemoveLatestFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<TsKvEntry> latestFuture = service.submit(() -> doFindLatestSync(entityId, query.getKey()));
        return Futures.transformAsync(latestFuture, latest -> {
            if (latest == null) {
                return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), false));
            }
            boolean isRemoved = false;
            Long version = null;
            long ts = latest.getTs();
            if (ts >= query.getStartTs() && ts < query.getEndTs()) {
                if (CitusShardRouter.isRouting(kvShardRouter)) {
                    version = kvShardRouter.routedQuery(entityId.getId(), removeLatestWithVersionQuery,
                            rs -> rs.next() ? rs.getObject(1, Long.class) : null,
                            entityId.getId(), keyDictionaryDao.getOrSaveKeyId(query.getKey()));
                } else {
                    version = transactionTemplate.execute(status -> jdbcTemplate.query(removeLatestWithVersionQuery,
                            rs -> rs.next() ? rs.getObject(1, Long.class) : null, entityId.getId(), keyDictionaryDao.getOrSaveKeyId(query.getKey())));
                }
                isRemoved = true;
                if (query.getRewriteLatestIfDeleted()) {
                    return getNewLatestEntryFuture(tenantId, entityId, query, version);
                }
            }
            return Futures.immediateFuture(new TsKvLatestRemovingResult(query.getKey(), isRemoved, version));
        }, MoreExecutors.directExecutor());
    }

    protected ListenableFuture<List<TsKvEntry>> getFindAllLatestFuture(EntityId entityId) {
        if (CitusShardRouter.isRouting(kvShardRouter)) {
            return service.submit(() -> {
                List<TsKvLatestEntity> rows = kvShardRouter.routedQuery(
                        entityId.getId(), FIND_ALL_TS_KV_LATEST_BY_ENTITY_QUERY, TS_KV_LATEST_WITH_STR_KEY_ROW_MAPPER, entityId.getId());
                return DaoUtil.convertDataList(rows);
            });
        }
        return service.submit(() ->
                DaoUtil.convertDataList(Lists.newArrayList(
                        searchTsKvLatestRepository.findAllByEntityId(entityId.getId()))));
    }

    protected ListenableFuture<Long> getSaveLatestFuture(EntityId entityId, TsKvEntry tsKvEntry) {
        TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityId(entityId.getId());
        latestEntity.setTs(tsKvEntry.getTs());
        latestEntity.setKey(keyDictionaryDao.getOrSaveKeyId(tsKvEntry.getKey()));
        latestEntity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        latestEntity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        latestEntity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        latestEntity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        latestEntity.setJsonValue(tsKvEntry.getJsonValue().orElse(null));

        return tsLatestQueue.add(latestEntity);
    }

    protected TsKvEntry wrapNullTsKvEntry(final String key, final TsKvEntry latest) {
        if (latest == null) {
            return new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
        }
        return latest;
    }

}
