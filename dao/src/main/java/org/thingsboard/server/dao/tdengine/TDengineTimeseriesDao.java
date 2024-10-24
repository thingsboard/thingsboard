/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.tdengine;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sqlts.AggregationTimeseriesDao;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.TDengineTsDao;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.EntityType.DEVICE;
import static org.thingsboard.server.common.data.kv.DataType.JSON;
import static org.thingsboard.server.common.data.kv.DataType.STRING;

/**
 * for tdengine
 */
@Slf4j
@Component
@TDengineTsDao
public class TDengineTimeseriesDao implements TimeseriesDao, AggregationTimeseriesDao {

    protected static final long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);

    @Autowired
    protected ScheduledLogExecutorComponent logExecutor;

    @Autowired
    private StatsFactory statsFactory;

    @Autowired
    @Qualifier("TDengineTemplate")
    protected JdbcTemplate template;

    @Autowired
    @Qualifier("TDengineDataSource")
    private DataSource dataSource;

    @Autowired
    private BaseTDengine tDBase;

    @Autowired
    protected JpaExecutorService service;

    protected TbSqlBlockingQueueWrapper<SQLUnit, Long> tsQueue;

    @Value("${sql.ts.batch_size:1000}")
    protected int tsBatchSize;
    @Value("${sql.ts.batch_max_delay:100}")
    protected long tsMaxDelay;
    @Value("${sql.ts.stats_print_interval_ms:1000}")
    protected long tsStatsPrintIntervalMs;
    @Value("${sql.ts.batch_threads:4}")
    protected int batchThreads;
    @Value("${sql.ttl.ts.ts_key_value_ttl:0}")
    private long systemTtl;
    @Value("${tdengine.useStable:false}")
    protected boolean useStable;
    @Value("${tdengine.stringColumnMaxLength:65517}")
    protected int stringColumnMaxLength;
    @Value("${tdengine.stringTagMaxLength:16382}")
    protected int stringTagMaxLength;
    @Value("${tdengine.processOnOverlimit:1}")
    protected int processOnOverlimit;

    @PostConstruct
    protected void init() {
        TbSqlBlockingQueueParams tsParams = TbSqlBlockingQueueParams.builder().logName("TS " + "TDengine").batchSize(tsBatchSize).maxDelay(tsMaxDelay).statsPrintIntervalMs(tsStatsPrintIntervalMs).statsNamePrefix("ts.tdengine").batchSortEnabled(false).build();
        tsQueue = new TbSqlBlockingQueueWrapper<>(tsParams, unit -> unit.getEntity().hashCode(), batchThreads, statsFactory);
        tsQueue.init(logExecutor, v -> {
            StringBuilder sqlStr = new StringBuilder("insert into");
            for (int i = 0; i < v.size(); i++) {
                sqlStr.append(" `").append(v.get(i).getEntity()).append("` (");
                sqlStr.append("ts").append(v.get(i).getKeySql()).append(") values (");
                sqlStr.append(v.get(i).getTime()).append(v.get(i).getValSql()).append(")");
                if ((i + 1) % 500 == 0) {
                    log.debug("execute sql: {}", sqlStr);
                    // error handling "schema is old"
                    int retry = 3;
                    while (retry-- > 0) {
                        try {
                            template.execute(sqlStr.toString());
                            break;
                        } catch (Exception e) {
                            log.error("error occurred while writing timeseries data, retry times {} remaining", retry);
                        }
                    }
                    sqlStr = new StringBuilder("insert into");
                }
            }
            if (v.size() % 500 > 0) {
                log.debug("execute sql: {}", sqlStr);
                // error handling "schema is old"
                int retry = 3;
                while (retry-- > 0) {
                    try {
                        template.execute(sqlStr.toString());
                        break;
                    } catch (Exception e) {
                        log.error("error occurred while writing timeseries data, retry times {} remaining", retry);
                    }
                }
            }
        }, (l, r) -> 0);
    }

    @PreDestroy
    protected void destroy() {
        if (tsQueue != null) {
            tsQueue.destroy();
        }
    }

    protected void printWarnings(Statement statement) throws SQLException {
        SQLWarning warnings = statement.getWarnings();
        if (warnings != null) {
            log.debug("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.debug("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }

    @Override
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<ReadTsKvQueryResult>> futures = queries.stream().map(query -> findAllAsync(tenantId, entityId, query)).collect(Collectors.toList());
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(tenantId, entityId, query);
        } else {
            return findAllAsyncWithAggregation(tenantId, entityId, query);
        }
    }

    private ListenableFuture<ReadTsKvQueryResult> findAllAsyncWithAggregation(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        // 表名、列名，忽略首尾空格
        String tableName = entityId.getId().toString().trim();
        String key = query.getKey().trim();
        // 查询表结构信息
        Map<String, BaseTDengine.Field> allFields = tDBase.getTableMetaCache(tableName);
        if (allFields == null || !allFields.containsKey(key)) {
            return Futures.immediateFuture(null);
        }
        // 执行语句
        String sql = "";
        // 判断聚合类型
        switch (query.getAggregation()) {
            case AVG:
                sql = "select avg(`" + key.trim() + "`) as avg_val, _wstart, _wend from `" + tableName + "` where _C0 >= " + query.getStartTs() + " and _C0 < " + query.getEndTs() + " interval(" + query.getInterval() + "a) order by _wstart " + ("ASC".equalsIgnoreCase(query.getOrder()) ? "ASC" : "DESC");
                break;
            case MAX:
                sql = "select max(`" + key.trim() + "`) as max_val, _wstart, _wend from `" + tableName + "` where _C0 >= " + query.getStartTs() + " and _C0 < " + query.getEndTs() + " interval(" + query.getInterval() + "a) order by _wstart " + ("ASC".equalsIgnoreCase(query.getOrder()) ? "ASC" : "DESC");
                break;
            case MIN:
                sql = "select min(`" + key.trim() + "`) as min_val, _wstart, _wend from `" + tableName + "` where _C0 >= " + query.getStartTs() + " and _C0 < " + query.getEndTs() + " interval(" + query.getInterval() + "a) order by _wstart " + ("ASC".equalsIgnoreCase(query.getOrder()) ? "ASC" : "DESC");
                break;
            case SUM:
                sql = "select sum(`" + key.trim() + "`) as sum_val, _wstart, _wend from `" + tableName + "` where _C0 >= " + query.getStartTs() + " and _C0 < " + query.getEndTs() + " interval(" + query.getInterval() + "a) order by _wstart " + ("ASC".equalsIgnoreCase(query.getOrder()) ? "ASC" : "DESC");
                break;
            case COUNT:
                sql = "select count(`" + key.trim() + "`) as count_val, _wstart, _wend from `" + tableName + "` where _C0 >= " + query.getStartTs() + " and _C0 < " + query.getEndTs() + " interval(" + query.getInterval() + "a) order by _wstart " + ("ASC".equalsIgnoreCase(query.getOrder()) ? "ASC" : "DESC");
                break;
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + query.getAggregation());
        }
        // 结果集
        List<TsKvEntry> entries = new ArrayList<>();
        template.query(sql, rs -> {
            ResultSetMetaData metaData = rs.getMetaData();
            String typeName = metaData.getColumnTypeName(1);
            String colName = metaData.getColumnName(1);
            // 判断聚合结果的数据类型
            switch (typeName) {
                case "DOUBLE":
                case "FLOAT":
                    do {
                        long _wstart = rs.getLong("_wstart");
                        long _wend = rs.getLong("_wend");
                        long ts = (_wstart + _wend) / 2;
                        DoubleDataEntry entry = new DoubleDataEntry(key, rs.getDouble(colName));
                        entries.add(new BasicTsKvEntry(ts, entry));
                    } while (rs.next());
                    break;
                default:
                    do {
                        long _wstart = rs.getLong("_wstart");
                        long _wend = rs.getLong("_wend");
                        long ts = (_wstart + _wend) / 2;
                        LongDataEntry entry = new LongDataEntry(key, rs.getLong(colName));
                        entries.add(new BasicTsKvEntry(ts, entry));
                    } while (rs.next());
                    break;
            }
        });
        long maxTs = entries.stream().mapToLong(TsKvEntry::getTs).max().orElse(0);
        return Futures.immediateFuture(new ReadTsKvQueryResult(query.getId(), entries, maxTs));
    }

    ListenableFuture<TsKvEntry> findAndAggregateAsync(EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation) {
        CompletableFuture<KvEntry> entityFuture = new CompletableFuture<>();
        switchAggregation(entityId, key, startTs, endTs, aggregation, entityFuture);
        return Futures.transform(setFuture(entityFuture), entity -> entity == null ? null : new BasicTsKvEntry(ts, entity), MoreExecutors.directExecutor());
    }

    protected void switchAggregation(EntityId entityId, String key, long startTs, long endTs, Aggregation aggregation, CompletableFuture<KvEntry> entryFuture) {
        String tableName = entityId.getId().toString().trim();
        Map<String, BaseTDengine.Field> allFields = tDBase.getTableMetaCache(tableName);
        if (allFields == null || !allFields.containsKey(key)) {
            entryFuture.complete(null);
        }
        switch (aggregation) {
            case AVG:
                findAvg(entityId, key, startTs, endTs, entryFuture);
                break;
            case MAX:
                findMax(entityId, key, startTs, endTs, entryFuture);
                break;
            case MIN:
                findMin(entityId, key, startTs, endTs, entryFuture);
                break;
            case SUM:
                findSum(entityId, key, startTs, endTs, entryFuture);
                break;
            case COUNT:
                findCount(entityId, key, startTs, endTs, entryFuture);
                break;
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    protected void findCount(EntityId entityId, String key, long startTs, long endTs, CompletableFuture<KvEntry> entryFuture) {
        String tableName = entityId.getId().toString().trim();
        String sql = "select count (`" + key.trim() + "`) as count_val from `" + tableName + "` where _C0 >= " + startTs + " and _C0 < " + endTs;
        try {
            template.query(sql, rs -> {
                entryFuture.complete(getResultKv(rs, key.trim()));
            });
            entryFuture.complete(null);
        } catch (Exception e) {
            entryFuture.completeExceptionally(e);
        }
    }

    protected void findSum(EntityId entityId, String key, long startTs, long endTs, CompletableFuture<KvEntry> entryFuture) {
        String tableName = entityId.getId().toString().trim();
        String sql = "select sum (`" + key.trim() + "`) as sum_val from `" + tableName + "` where _C0 >= " + startTs + " and _C0 < " + endTs;
        try {
            template.query(sql, rs -> {
                entryFuture.complete(getResultKv(rs, key.trim()));
            });
            entryFuture.complete(null);
        } catch (Exception e) {
            entryFuture.completeExceptionally(e);
        }
    }

    protected void findMin(EntityId entityId, String key, long startTs, long endTs, CompletableFuture<KvEntry> entryFuture) {
        String tableName = entityId.getId().toString().trim();
        String sql = "select min (`" + key.trim() + "`) as min_val from `" + tableName + "` where _C0 >= " + startTs + " and _C0 < " + endTs;
        try {
            template.query(sql, rs -> {
                entryFuture.complete(getResultKv(rs, key.trim()));
            });
            entryFuture.complete(null);
        } catch (Exception e) {
            entryFuture.completeExceptionally(e);
        }
    }

    protected void findMax(EntityId entityId, String key, long startTs, long endTs, CompletableFuture<KvEntry> entryFuture) {
        String tableName = entityId.getId().toString().trim();
        String sql = "select max (`" + key.trim() + "`) as max_val from `" + tableName + "` where _C0 >= " + startTs + " and _C0 < " + endTs;
        try {
            template.query(sql, rs -> {
                entryFuture.complete(getResultKv(rs, key.trim()));
            });
            entryFuture.complete(null);
        } catch (Exception e) {
            entryFuture.completeExceptionally(e);
        }
    }

    protected void findAvg(EntityId entityId, String key, long startTs, long endTs, CompletableFuture<KvEntry> entryFuture) {
        String tableName = entityId.getId().toString().trim();
        String sql = "select avg (`" + key.trim() + "`) as avg_val from `" + tableName + "` where _C0 >= " + startTs + " and _C0 < " + endTs;
        try {
            template.query(sql, rs -> {
                entryFuture.complete(getResultKv(rs, key.trim()));
            });
            entryFuture.complete(null);
        } catch (Exception e) {
            entryFuture.completeExceptionally(e);
        }
    }

    private KvEntry getResultKv(ResultSet rs, String key) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        String typeName = metaData.getColumnTypeName(1);
        String colName = metaData.getColumnName(1);
        key = key.trim();
        colName = colName.trim();
        switch (typeName) {
            case "NCHAR":
            case "VARCHAR":
            case "BINARY":
                return new StringDataEntry(key, rs.getString(colName));
            case "DOUBLE":
            case "FLOAT":
                return new DoubleDataEntry(key, rs.getDouble(colName));
            case "BOOL":
                return new BooleanDataEntry(key, rs.getBoolean(colName));
            default:
                return new LongDataEntry(key, rs.getLong(colName));
        }
    }

    private ListenableFuture<ReadTsKvQueryResult> findAllAsyncWithLimit(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        String tableName = entityId.getId().toString().trim();
        Map<String, BaseTDengine.Field> allFields = tDBase.getTableMetaCache(tableName);
        if (allFields == null || !allFields.containsKey(query.getKey())) {
            return Futures.immediateFuture(null);
        }
        String taosType = allFields.get(query.getKey()).getType();
        if (taosType == null) {
            return Futures.immediateFuture(null);
        }
        if (query.getLimit() < 1) {
            throw new IllegalArgumentException("Page size must not be less than one!");
        }
        List<TsKvEntry> entries = new ArrayList<>();
        String key = query.getKey().trim();
        String sql = "select _C0 as ts, `" + key + "` from `" + tableName + "` where `" + key + "` is not null and _C0 >= " + query.getStartTs() + " and _C0 < " + query.getEndTs() + " order by ts " + ("ASC".equalsIgnoreCase(query.getOrder()) ? "ASC" : "DESC") + " limit " + query.getLimit();
        template.query(sql, rs -> {
            switch (taosType) {
                case "NCHAR":
                case "VARCHAR":
                case "BINARY":
                    do {
                        StringDataEntry entry = new StringDataEntry(key, rs.getString(key));
                        entries.add(new BasicTsKvEntry(rs.getLong("ts"), entry));
                    } while (rs.next());
                    break;
                case "BOOL":
                    do {
                        BooleanDataEntry entry = new BooleanDataEntry(key, rs.getBoolean(key));
                        entries.add(new BasicTsKvEntry(rs.getLong("ts"), entry));
                    } while (rs.next());
                    break;
                default:
                    do {
                        DoubleDataEntry entry = new DoubleDataEntry(key, rs.getDouble(key));
                        entries.add(new BasicTsKvEntry(rs.getLong("ts"), entry));
                    } while (rs.next());
            }
        });
        long maxTs = entries.stream().mapToLong(TsKvEntry::getTs).max().orElse(0);
        return Futures.immediateFuture(new ReadTsKvQueryResult(query.getId(), entries, maxTs));
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        int dataPointDays = getDataPointDays(tsKvEntry, computeTtl(ttl));
        String key = tsKvEntry.getKey().trim();
        String tableName = entityId.getId().toString().trim();
        // get Mate
        Map<String, BaseTDengine.Field> tableMeta = tDBase.getTableMetaCache(tableName);
        BaseTDengine.Field field;
        if (DEVICE == entityId.getEntityType() && useStable && (tableMeta.isEmpty() || tableMeta.get(key) == null)) {
            return Futures.immediateFuture(null);
        }
        if (tableMeta.size() < 1) {
            // create table
            tDBase.createTable(entityId, tsKvEntry);
            field = new BaseTDengine.Field(key, tDBase.varcharLen, false);
        } else {
            field = tableMeta.get(key);
            int retry = 3;
            while (null == field && retry-- > 0) {
                try {
                    tDBase.alterTable(entityId, tsKvEntry);
                    field = tableMeta.get(key);
                    Thread.sleep(10L);
                } catch (Exception ex) {
                    log.error("alterTable in TDengineTimeseriesDao occurred exception", ex);
                }
            }
            if (null == field) {
                field = new BaseTDengine.Field(key, tDBase.varcharLen, false);
            }
        }
        String value = tsKvEntry.getValueAsString();
        String sql = null;
        if (field.isTag()) {
            /* process on over limit start, add at 2023.05.15 */
            if (tsKvEntry.getDataType() == STRING || tsKvEntry.getDataType() == JSON) {
                if (value.length() > this.stringTagMaxLength) {
                    if (this.processOnOverlimit == 2) {
                        // 丢弃
                        return Futures.immediateFuture(null);
                    } else {
                        // 截取
                        value = value.substring(0, this.stringTagMaxLength);
                    }
                }
                value = "'" + value.replaceAll("'", "\"") + "'";
            }
            /* process on over limit end, add at 2023.05.15 */
            sql = "alter table `" + tableName + "` set tag `" + key + "`=" + value;
            template.execute(sql);
            tDBase.updateTableMetaCache(tableName);
            return Futures.immediateFuture(null);
        }
        if (tsKvEntry.getDataType() == STRING || tsKvEntry.getDataType() == JSON) {
            /* process on over limit start, add at 2023.05.15 */
            if (value.length() > this.stringColumnMaxLength) {
                if (this.processOnOverlimit == 2) {
                    // 丢弃
                    return Futures.immediateFuture(null);
                } else {
                    // 截取
                    value = value.substring(0, this.stringColumnMaxLength);
                }
            }
            /* process on over limit end, add at 2023.05.15 */
            if (value.length() > field.getLength()) {
                tDBase.resizeBinaryColumn(entityId, tsKvEntry, value.length());
            }
            value = "'" + value.replaceAll("'", "\"") + "'";
        }
        sql = "insert into `" + tableName + "` (ts, `" + key + "`) values (" + tsKvEntry.getTs() + ", " + value + ")";
        return Futures.transform(tsQueue.add(new SQLUnit(sql, tableName)), v -> dataPointDays, MoreExecutors.directExecutor());
    }

    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        String tableName = entityId.getId().toString().trim();
        if (null == tsKvEntries || tsKvEntries.size() == 0) {
            return Futures.immediateFuture(null);
        }
        int dataPointDays = getDataPointDays(tsKvEntries.get(0), computeTtl(ttl));
        Map<String, BaseTDengine.Field> tableMeta = tDBase.getTableMetaCache(tableName);
        Map<String, BaseTDengine.Field> finalMeta = tableMeta;
        var ref = new Object() {
            Map<String, BaseTDengine.Field> finalTableMeta = finalMeta;
        };
        if (DEVICE == entityId.getEntityType() && useStable && (tableMeta.isEmpty() || tsKvEntries.stream().filter(e -> ref.finalTableMeta.get(e.getKey().trim()) == null).count() > 0)) {
            return Futures.immediateFuture(null);
        }
        if (tableMeta.size() < 1) {
            // create table
            tDBase.createTable(entityId, tsKvEntries);
            ref.finalTableMeta = tDBase.getTableMetaCache(tableName);
        }
        StringBuilder keySql = new StringBuilder();
        StringBuilder valSql = new StringBuilder();
        tsKvEntries.forEach(e -> {
            String key = e.getKey().trim();
            BaseTDengine.Field f = ref.finalTableMeta.get(key);
            // unused field always be 73
            if ("t1".equals(key)) {
                return;
            }
            int retry = 3;
            while (null == f && retry-- > 0) {
                try {
                    tDBase.alterTable(entityId, e);
                    ref.finalTableMeta = tDBase.getTableMetaCache(tableName);
                    f = ref.finalTableMeta.get(key);
                    Thread.sleep(10L);
                } catch (Exception ex) {
                    log.error("alterTable in TDengineTimeseriesDao occurred exception", ex);
                }
            }
            String value = e.getValueAsString();
            if (f.isTag()) {
                if (e.getDataType() == STRING || e.getDataType() == JSON) {
                    if (value.length() > this.stringTagMaxLength) {
                        if (this.processOnOverlimit == 2) {
                            // 丢弃
                            return;
                        } else {
                            // 截取
                            value = value.substring(0, this.stringTagMaxLength);
                        }
                    }
                    value = "'" + value.replaceAll("'", "\"") + "'";
                }
                String s = "alter table `" + tableName + "` set tag `" + key + "`=" + value;
                template.execute(s);
                ref.finalTableMeta = tDBase.updateTableMetaCache(tableName);
            }
            if (ref.finalTableMeta.get(key).isTag()) {
                return;
            }
            if (e.getDataType() == STRING || e.getDataType() == JSON) {
                /* process on over limit start, add at 2023.05.15 */
                if (value.length() > this.stringColumnMaxLength) {
                    if (this.processOnOverlimit == 2) {
                        // 丢弃
                        return;
                    } else {
                        // 截取
                        value = value.substring(0, this.stringColumnMaxLength);
                    }
                }
                /* process on over limit end, add at 2023.05.15 */
                keySql.append(", `").append(key).append("`");
                valSql.append(", '").append(value.replaceAll("'", "\"")).append("'");
                if (value.length() > ref.finalTableMeta.get(key).getLength()) {
                    tDBase.resizeBinaryColumn(entityId, e, value.length());
                    // ref.finalTableMeta = tDBase.updateTableMetaCache(tableName);
                    ref.finalTableMeta = tDBase.getTableMetaCache(tableName);
                }
            } else {
                keySql.append(", `").append(key).append("`");
                valSql.append(", ").append(e.getValue());
            }
        });
        if (StringUtils.isEmpty(keySql)) {
            return Futures.immediateFuture(null);
        }
        return Futures.transform(tsQueue.add(new SQLUnit(tableName, tsKvEntries.get(0).getTs(), keySql.toString(), valSql.toString())), v -> dataPointDays, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Integer> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return null;
    }

    @Override
    public void cleanup(long systemTtl) {
        log.error("cleanup...");
    }

    protected long computeTtl(long ttl) {
        if (systemTtl > 0) {
            if (ttl == 0) {
                ttl = systemTtl;
            } else {
                ttl = Math.min(systemTtl, ttl);
            }
        }
        return ttl;
    }

    protected int getDataPointDays(TsKvEntry tsKvEntry, long ttl) {
        return tsKvEntry.getDataPoints() * Math.max(1, (int) (ttl / SECONDS_IN_DAY));
    }

    protected SettableFuture<KvEntry> setFuture(CompletableFuture<KvEntry> entryFuture) {
        SettableFuture<KvEntry> listenableFuture = SettableFuture.create();
        entryFuture.whenComplete((kvEntry, throwable) -> {
            if (throwable != null) {
                listenableFuture.setException(throwable);
            } else {
                listenableFuture.set(kvEntry);
            }
        });
        return listenableFuture;
    }
}