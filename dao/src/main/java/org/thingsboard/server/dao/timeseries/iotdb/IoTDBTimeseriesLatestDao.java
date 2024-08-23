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
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;
import org.thingsboard.server.dao.util.IoTDBTsLatestDao;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@IoTDBTsLatestDao
public class IoTDBTimeseriesLatestDao implements TimeseriesLatestDao {

    @Autowired
    private IoTDBBaseTimeseriesDao ioTDBBaseTimeseriesDao;
    @Override
    public ListenableFuture<Optional<TsKvEntry>> findLatestOpt(TenantId tenantId, EntityId entityId, String key) {
        return Futures.submit(() -> {
            String sql = "select last `" + key + "` from "+ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId() + "` ";
            SessionDataSetWrapper sessionDataSetWrapper = ioTDBBaseTimeseriesDao.myIotdbSessionPool.executeQueryStatement(sql);
            String value = "";
            long time = System.currentTimeMillis();
            BasicKvEntry entry = new StringDataEntry(key, value);
            if (sessionDataSetWrapper.hasNext()) {
                RowRecord record = sessionDataSetWrapper.next();
                Field field = record.getFields().get(1);
                Field fieldType = record.getFields().get(2);
                time = record.getTimestamp();
                entry = getLastEntry(key, field,fieldType);
            }

            return Optional.of(new BasicTsKvEntry(time, entry));
        }, ioTDBBaseTimeseriesDao.readResultsProcessingExecutor);
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        return Futures.submit(() -> {
            String sql = "select last `" + key + "` from "+ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId() + "` ";
            SessionDataSetWrapper sessionDataSetWrapper = ioTDBBaseTimeseriesDao.myIotdbSessionPool.executeQueryStatement(sql);
            String value = "";
            long time = System.currentTimeMillis();
            BasicKvEntry entry = new StringDataEntry(key, value);
            if (sessionDataSetWrapper.hasNext()) {
                RowRecord record = sessionDataSetWrapper.next();
                Field field = record.getFields().get(1);
                Field fieldType = record.getFields().get(2);
                time = record.getTimestamp();
                entry = getLastEntry(key, field,fieldType);
            }
            return new BasicTsKvEntry(time, entry);
        }, ioTDBBaseTimeseriesDao.readResultsProcessingExecutor);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return Futures.submit(() -> {
            String sql = "select last *  from "+ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId() + "` ";
            SessionDataSetWrapper sessionDataSetWrapper = ioTDBBaseTimeseriesDao.myIotdbSessionPool.executeQueryStatement(sql);
            List<TsKvEntry> list = Lists.newArrayList();
            while (sessionDataSetWrapper.hasNext()) {
                RowRecord record = sessionDataSetWrapper.next();
                Field fieldName = record.getFields().get(0);
                Field field = record.getFields().get(1);
                Field fieldType = record.getFields().get(2);
                String key = fieldName.getStringValue().substring(fieldName.getStringValue().lastIndexOf('.') + 1);
                BasicKvEntry basicKvEntry = getLastEntry(key, field, fieldType);
                BasicTsKvEntry tsKvEntry = new BasicTsKvEntry(record.getTimestamp(), basicKvEntry);
                list.add(tsKvEntry);
            }
            return list;
        }, ioTDBBaseTimeseriesDao.readResultsProcessingExecutor);
    }

    @Override
    public ListenableFuture<Long> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        if (null != tsKvEntry.getJsonValue().orElse(null)) {
            return Futures.immediateFuture(null);
        }
        try {
            ioTDBBaseTimeseriesDao.myIotdbSessionPool.insertRecord(ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId()+"`", tsKvEntry.getTs(), Lists.newArrayList(tsKvEntry.getKey()), Lists.newArrayList(getType(tsKvEntry)), Lists.newArrayList(tsKvEntry.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Futures.immediateFuture(null);
    }


    public BasicKvEntry getLastEntry(String key , Field field, Field fieldType) {
        if(TSDataType.BOOLEAN.name().equals(fieldType.getStringValue())){
            return new BooleanDataEntry(key,Boolean.valueOf(field.getStringValue()));
        }
        if(TSDataType.INT64.name().equals(fieldType.getStringValue())){
            return new LongDataEntry(key,Long.valueOf(field.getStringValue()));
        }
        if(TSDataType.DOUBLE.name().equals(fieldType.getStringValue())){
            return new DoubleDataEntry(key,Double.valueOf(field.getStringValue()));
        }
        if(TSDataType.TEXT.name().equals(fieldType.getStringValue())){
            return new StringDataEntry(key,field.getStringValue());
        }
        return new StringDataEntry(key,field.getStringValue());
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
        return TSDataType.TEXT;
    }

    @Override
    public ListenableFuture<TsKvLatestRemovingResult> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return Futures.submit(() -> {
            String sql = "select last `" + query.getKey() + "`  from "+ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId() + "` ";
            SessionDataSetWrapper sessionDataSetWrapper = ioTDBBaseTimeseriesDao.myIotdbSessionPool.executeQueryStatement(sql);
            while (sessionDataSetWrapper.hasNext()) {
                long time = sessionDataSetWrapper.next().getTimestamp();
                ioTDBBaseTimeseriesDao.myIotdbSessionPool.deleteData(ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId() + "`.`" + query.getKey() + "`", time);
            }
            return new TsKvLatestRemovingResult(query.getKey(), true);
        }, ioTDBBaseTimeseriesDao.readResultsProcessingExecutor);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return Lists.newArrayList();
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        List<String> list = Lists.newArrayList();
        entityIds.forEach(entityId -> {
            String sql = "select last *  from "+ioTDBBaseTimeseriesDao.getDbName()+".`" + entityId.getId() + "` ";
            SessionDataSetWrapper sessionDataSetWrapper = null;
            try {
                sessionDataSetWrapper = ioTDBBaseTimeseriesDao.myIotdbSessionPool.executeQueryStatement(sql);
                while (sessionDataSetWrapper.hasNext()) {
                    RowRecord record = sessionDataSetWrapper.next();
                    String timeseries = record.getFields().get(0).getStringValue();
                    String key = timeseries.substring(timeseries.lastIndexOf('.') + 1);
                    list.add(key);
                }
            } catch (IoTDBConnectionException | StatementExecutionException e) {
                e.printStackTrace();
            }
        });
        return list;
    }
}
