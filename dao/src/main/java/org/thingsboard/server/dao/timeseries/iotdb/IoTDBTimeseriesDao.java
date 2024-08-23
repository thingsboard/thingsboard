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

package org.thingsboard.server.dao.timeseries.iotdb;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.isession.pool.SessionDataSetWrapper;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.Field;
import org.apache.iotdb.tsfile.read.common.RowRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.sqlts.AggregationTimeseriesDao;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.IoTDBTsDao;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@IoTDBTsDao
public class IoTDBTimeseriesDao implements TimeseriesDao,AggregationTimeseriesDao{


    protected static final int MIN_AGGREGATION_STEP_MS = 1000;

    protected static final int MAX_SIZE = 1000;

    @Autowired
    private IoTDBBaseTimeseriesDao ioTDBBaseTimeseriesDao;

    @Override
    public ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        String sql;
        if (query.getAggregation().equals(Aggregation.NONE)) {
            sql = findAllAsyncWithLimit(tenantId, entityId, query);
        } else {
            sql = findAllAsyncWithAgg(tenantId, entityId, query);
        }

        return Futures.submit(() -> {
            SessionDataSetWrapper sessionDataSetWrapper = ioTDBBaseTimeseriesDao.myIotdbSessionPool.executeQueryStatement(sql);
            List<TsKvEntry> data = Lists.newArrayList();
            while (sessionDataSetWrapper.hasNext()) {
                RowRecord record = sessionDataSetWrapper.next();
                Field field = record.getFields().get(0);
                BasicKvEntry basicKvEntry = ioTDBBaseTimeseriesDao.getEntry(query.getKey(),field);
                BasicTsKvEntry tsKvEntry = new BasicTsKvEntry(record.getTimestamp(), basicKvEntry);
                data.add(tsKvEntry);
            }
            return new ReadTsKvQueryResult(query.getId(), data, System.currentTimeMillis());
        }, ioTDBBaseTimeseriesDao.readResultsProcessingExecutor);

    }

    private String findAllAsyncWithAgg(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        String sql;
        long start = query.getStartTs();
        long end = Math.max(query.getStartTs() + 1, query.getEndTs());
        if(query.getInterval() >0){
            Long step = Math.max(query.getInterval(), MIN_AGGREGATION_STEP_MS);
            start = System.currentTimeMillis();
            end = start + step;
        }

        sql = "select " + transferAgg(query) + " from "+ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId() + "` ";
        if (start != 0 && end != 0) {
            sql = sql + "where time > " + start + " and time < " + end;
        } else if (start != 0) {
            sql = sql + "where time > " + start;
        } else if (end != 0) {
            sql = sql + "where time < " + end;
        }
        return sql;
    }

    private String transferAgg(ReadTsKvQuery query) {
        if (query.getAggregation().equals(Aggregation.MAX)) {
            return " MAX_VALUE(`" + query.getKey() + "`) ";
        }
        if (query.getAggregation().equals(Aggregation.MIN)) {
            return " MIN_VALUE(`" + query.getKey() + "`) ";
        }
        if (query.getAggregation().equals(Aggregation.COUNT)) {
            return " COUNT(`" + query.getKey() + "`) ";
        }
        if (query.getAggregation().equals(Aggregation.AVG)) {
            return " AVG(`" + query.getKey() + "`) ";
        }
        if (query.getAggregation().equals(Aggregation.SUM)) {
            return " SUM(`" + query.getKey() + "`) ";
        }
        return "";
    }

    private String findAllAsyncWithLimit(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {

        String sql = "select `" + query.getKey()+ "` from "+ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId() + "` ";
        long start = query.getStartTs();
        long end = Math.max(query.getStartTs() + 1, query.getEndTs());

        if(query.getInterval() >0){
            Long step = Math.max(query.getInterval(), MIN_AGGREGATION_STEP_MS);
            start = System.currentTimeMillis();
            end = start + step;
        }

        if (start != 0 && end != 0) {
            sql = sql + "where time > " + start + " and time < " + end;
        } else if (start != 0) {
            sql = sql + "where time > " + start;
        } else if (end != 0) {
            sql = sql + "where time < " + end;
        }

        String order = query.getOrder();
        if (StringUtils.isNoneBlank(order)) {
            sql = sql + " order by time " + order;
        }
        int limit = query.getLimit();
        if (limit == 0) {
            sql = sql + " limit " + MAX_SIZE;
        } else {
            sql = sql + " limit " + limit;
        }
        return sql;

    }

    @Override
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return Futures.submit(() -> {
            return queries.stream()
                    .map(query -> {
                        String sql;
                        if (query.getAggregation().equals(Aggregation.NONE)) {
                            sql = findAllAsyncWithLimit(tenantId, entityId, query);
                        } else {
                            sql = findAllAsyncWithAgg(tenantId, entityId, query);
                        }
                        SessionDataSetWrapper sessionDataSetWrapper = null;
                        List<TsKvEntry> data = Lists.newArrayList();
                        try {
                            sessionDataSetWrapper = ioTDBBaseTimeseriesDao.myIotdbSessionPool.executeQueryStatement(sql);
                            while (sessionDataSetWrapper.hasNext()) {
                                RowRecord record = sessionDataSetWrapper.next();
                                Field field = record.getFields().get(0);
                                BasicKvEntry basicKvEntry = ioTDBBaseTimeseriesDao.getEntry(query.getKey(),field);
                                BasicTsKvEntry tsKvEntry = new BasicTsKvEntry(record.getTimestamp(), basicKvEntry);
                                data.add(tsKvEntry);
                            }
                        } catch (IoTDBConnectionException e) {
                            e.printStackTrace();
                        } catch (StatementExecutionException e) {
                            e.printStackTrace();
                        }
                        return new ReadTsKvQueryResult(query.getId(), data, System.currentTimeMillis());
                    }).collect(Collectors.toList());
        }, ioTDBBaseTimeseriesDao.readResultsProcessingExecutor);
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        if (null != tsKvEntry.getJsonValue().orElse(null)) {
            return Futures.immediateFuture(0);
        }
        try {
            ioTDBBaseTimeseriesDao.myIotdbSessionPool.insertRecord(ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId()+"`", tsKvEntry.getTs(), Lists.newArrayList(tsKvEntry.getKey()), Lists.newArrayList(getType(tsKvEntry)), Lists.newArrayList(tsKvEntry.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Futures.immediateFuture(1);
    }

    private TSDataType getType(TsKvEntry tsKvEntry) {
        if (null != tsKvEntry.getBooleanValue().orElse(null)) {
            return TSDataType.BOOLEAN;
        }
        if (null != tsKvEntry.getLongValue().orElse(null)) {
            return TSDataType.INT64;
        }
        if (null != tsKvEntry.getDoubleValue().orElse(null)) {
            return TSDataType.DOUBLE;
        }
//        if (null != tsKvEntry.getStrValue().orElse(null)) {
//            return TSDataType.TEXT;
//        }
//        if (null != tsKvEntry.getJsonValue().orElse(null)) {
//
//        }
        return TSDataType.TEXT;
    }

    @Override
    public ListenableFuture<Integer> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String
            key) {
        return Futures.immediateFuture(0);
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return Futures.submit(() -> {
            try {
                ioTDBBaseTimeseriesDao.myIotdbSessionPool.deleteData(Lists.newArrayList(ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId() + "`." + query.getKey()), query.getStartTs(), query.getEndTs());
            } catch (IoTDBConnectionException e) {
                e.printStackTrace();
            } catch (StatementExecutionException e) {
                e.printStackTrace();
            }
        }, ioTDBBaseTimeseriesDao.readResultsProcessingExecutor);
    }

    @Override
    public void cleanup(long systemTtl) {
        try {
            ioTDBBaseTimeseriesDao.myIotdbSessionPool.deleteData(Lists.newArrayList(ioTDBBaseTimeseriesDao.getDbName()+".**"), systemTtl, System.currentTimeMillis());
        } catch (IoTDBConnectionException e) {
            e.printStackTrace();
        } catch (StatementExecutionException e) {
            e.printStackTrace();
        }
    }

}
