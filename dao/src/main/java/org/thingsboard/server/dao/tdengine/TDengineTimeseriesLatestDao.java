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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;
import org.thingsboard.server.dao.util.TDengineTsLatestDao;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.EntityType.DEVICE;
import static org.thingsboard.server.common.data.kv.DataType.JSON;
import static org.thingsboard.server.common.data.kv.DataType.STRING;

/**
 * for tdengine
 */
@Slf4j
@Component
@TDengineTsLatestDao
public class TDengineTimeseriesLatestDao implements TimeseriesLatestDao {

    private TbSqlBlockingQueueWrapper<SQLUnit, Long> tsLatestQueue;

    @Autowired
    @Qualifier("TDengineTemplate")
    protected JdbcTemplate template;

    @Autowired
    @Qualifier("TDengineDataSource")
    private DataSource dataSource;

    @Autowired
    private BaseTDengine tDBase;

    @Autowired
    protected ScheduledLogExecutorComponent logExecutor;

    @Autowired
    private StatsFactory statsFactory;

    @Value("${sql.ts_latest.batch_size:1000}")
    private int tsLatestBatchSize;
    @Value("${sql.ts_latest.batch_max_delay:100}")
    private long tsLatestMaxDelay;
    @Value("${sql.ts_latest.stats_print_interval_ms:1000}")
    private long tsLatestStatsPrintIntervalMs;
    @Value("${sql.ts_latest.batch_threads:4}")
    private int tsLatestBatchThreads;
    @Value("${sql.batch_sort:false}")
    protected boolean batchSortEnabled;
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
        TbSqlBlockingQueueParams tsLatestParams = TbSqlBlockingQueueParams.builder().logName("TDengine Latest").batchSize(tsLatestBatchSize).maxDelay(tsLatestMaxDelay).statsPrintIntervalMs(tsLatestStatsPrintIntervalMs).statsNamePrefix("ts.latest").batchSortEnabled(false).build();
        tsLatestQueue = new TbSqlBlockingQueueWrapper<>(tsLatestParams, unit -> unit.getEntity().hashCode(), tsLatestBatchThreads, statsFactory);
        tsLatestQueue.init(logExecutor, v -> {
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
                            log.error("error occurred while writing timeseries latest data, retry times {} remaining", retry);
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
                        log.error("error occurred while writing timeseries latest data, retry times {} remaining", retry);
                    }
                }
            }
        }, (l, r) -> 0);
    }

    @PreDestroy
    protected void destroy() {
        if (tsLatestQueue != null) {
            tsLatestQueue.destroy();
        }
    }

    @Override
    public ListenableFuture<Optional<TsKvEntry>> findLatestOpt(TenantId tenantId, EntityId entityId, String key) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ListenableFuture<TsKvEntry> future = findLatest(tenantId, entityId, key);
        return Futures.transform(future, input -> Optional.ofNullable(input), executor);
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        String tableName = entityId.getId().toString().trim();
        Map<String, BaseTDengine.Field> allFields = tDBase.getTableMetaCache(tableName);
        if (!allFields.containsKey(key.trim())) {
            return Futures.immediateFuture(new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key.trim(), null)));
        }
        String taosType = allFields.get(key.trim()).getType();
        if (taosType == null) {
            return Futures.immediateFuture(new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key.trim(), null)));
        }
        final TsKvEntry[] entry = new TsKvEntry[1];
        String sql = "select last(ts) as ts, last(`" + key.trim() + "`) as `" + key.trim() + "` from `" + tableName + "`";
        template.query(sql, rs -> {
            switch (taosType) {
                case "NCHAR":
                case "VARCHAR":
                case "BINARY": {
                    StringDataEntry dataEntry = new StringDataEntry(key.trim(), rs.getString(key.trim()));
                    entry[0] = new BasicTsKvEntry(rs.getLong("ts"), dataEntry);
                    break;
                }
                case "BOOL": {
                    BooleanDataEntry dataEntry = new BooleanDataEntry(key.trim(), rs.getBoolean(key.trim()));
                    entry[0] = new BasicTsKvEntry(rs.getLong("ts"), dataEntry);
                    break;
                }
                default: {
                    DoubleDataEntry dataEntry = new DoubleDataEntry(key.trim(), rs.getDouble(key.trim()));
                    entry[0] = new BasicTsKvEntry(rs.getLong("ts"), dataEntry);
                }
            }
        });
        TsKvEntry result;
        if (entry[0] != null) {
            result = entry[0];
        } else {
            result = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key.trim(), null));
        }
        return Futures.immediateFuture(result);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        String tableName = entityId.getId().toString().trim();
        Map<String, BaseTDengine.Field> tableMeta = tDBase.getTableMetaCache(tableName);
        if (tableMeta.size() < 1) {
            return Futures.immediateFuture(Collections.emptyList());
        }
        List<TsKvEntry> entries = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select ");
        String collect = tableMeta.keySet().stream().map(k -> " last(`" + k.trim() + "`) as `" + k.trim() + "`").collect(Collectors.joining(","));
        sql.append(collect).append(" from `").append(tableName).append("`");
        log.info("find all latest sql: {}, entity: {}", sql.toString(), tableName);
        template.query(sql.toString(), (RowCallbackHandler) rs -> {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String key = metaData.getColumnName(i);
                key = key.trim();
                if ("ts".equals(key)) {
                    continue;
                }
                String taosType = tableMeta.get(key).getType();
                switch (taosType) {
                    case "NCHAR":
                    case "VARCHAR":
                    case "BINARY": {
                        StringDataEntry dataEntry = new StringDataEntry(key, rs.getString(key));
                        entries.add(new BasicTsKvEntry(rs.getLong("ts"), dataEntry));
                        break;
                    }
                    case "BOOL": {
                        BooleanDataEntry dataEntry = new BooleanDataEntry(key, rs.getBoolean(key));
                        entries.add(new BasicTsKvEntry(rs.getLong("ts"), dataEntry));
                        break;
                    }
                    default: {
                        // sql exception: invalid bigint data
                        // 设备最初的数据是 0，后面是 double 类型导致错误，此处统一按 double 处理
                        DoubleDataEntry dataEntry = new DoubleDataEntry(key, rs.getDouble(key));
                        entries.add(new BasicTsKvEntry(rs.getLong("ts"), dataEntry));
                    }
                }
            }
        });
        if (entries.isEmpty()) {
            return Futures.immediateFuture(Collections.emptyList());
        }
        return Futures.immediateFuture(entries);
    }

    @Override
    public ListenableFuture<Long> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        String key = tsKvEntry.getKey().trim();
        String tableName = entityId.getId().toString().trim();
        // get Mate
        Map<String, BaseTDengine.Field> tableMeta = tDBase.getTableMetaCache(tableName);
        BaseTDengine.Field field = null;
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
                    log.error("alterTable in TDengineTimeseriesLatestDao occurred exception", ex);
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
        return tsLatestQueue.add(new SQLUnit(sql, tableName));
    }

    public ListenableFuture<Long> saveLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries) {
        String tableName = entityId.getId().toString().trim();
        if (null == tsKvEntries || tsKvEntries.size() == 0) {
            return Futures.immediateFuture(null);
        }
        if (tsKvEntries.size() == 1) {
            return Futures.immediateFuture(null);
        }
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
                } catch (Exception ex) {
                    log.error("alterTable in TDengineTimeseriesLatestDao occurred exception", ex);
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
        return tsLatestQueue.add(new SQLUnit(tableName, tsKvEntries.get(0).getTs(), keySql.toString(), valSql.toString()));
    }

    @Override
    public ListenableFuture<TsKvLatestRemovingResult> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return Futures.immediateFuture(null);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return Collections.emptyList();
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        return Arrays.asList(entityIds.stream().flatMap(entity -> tDBase.getTableMetaCache(entity.getId().toString().trim()).keySet().stream()).distinct().toArray(String[]::new));
    }
}