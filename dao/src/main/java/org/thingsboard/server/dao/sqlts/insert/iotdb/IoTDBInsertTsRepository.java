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

package org.thingsboard.server.dao.sqlts.insert.iotdb;

import com.google.common.collect.Lists;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.iotdb.IoTDBTsKvEntity;
import org.thingsboard.server.dao.sqlts.insert.InsertTsRepository;
import org.thingsboard.server.dao.timeseries.iotdb.IoTDBBaseTimeseriesDao;
import org.thingsboard.server.dao.util.IoTDBTsDao;
import java.util.List;

@IoTDBTsDao
@Repository
@Transactional
public class IoTDBInsertTsRepository implements InsertTsRepository<IoTDBTsKvEntity> {

    @Autowired
    private IoTDBBaseTimeseriesDao ioTDBBaseTimeseriesDao;

    @Override
    public void saveOrUpdate(List<IoTDBTsKvEntity> entities) {
        List<String> deviceIds = Lists.newArrayList();
        List<Long> timesList = Lists.newArrayList();
        List<List<String>> measurementList = Lists.newArrayList();
        List<List<TSDataType>> tsDataTypesList = Lists.newArrayList();
        List<List<Object>> valuesList = Lists.newArrayList();
        entities.forEach(
                entity -> {
                    deviceIds.add(entity.getDevice());
                    timesList.add(entity.getTs());
                    measurementList.add(Lists.newArrayList(entity.getMeasurement()));
                    tsDataTypesList.add(Lists.newArrayList(entity.getTsDataType()));
                    valuesList.add(Lists.newArrayList(entity.getValue()));
                    }
        );
        try {
            ioTDBBaseTimeseriesDao.getIotdbSessionPool().insertRecords(deviceIds, timesList, measurementList, tsDataTypesList, valuesList);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
