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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import org.apache.iotdb.session.pool.SessionPool;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.Field;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.util.IoTDBAnyDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Configuration
@Data
@IoTDBAnyDao
public class IoTDBBaseTimeseriesDao {
    public ExecutorService readResultsProcessingExecutor = Executors.newFixedThreadPool(20);

    public SessionPool myIotdbSessionPool;

    @Value("${iotdb.host:localhost}")
    private String host;

    @Value("${iotdb.port:6667}")
    private int port;

    @Value("${iotdb.user:root}")
    private String user;

    @Value("${iotdb.password:root}")
    private String password;

    @Value("${iotdb.connection_timeout:5000}")
    private int timeout;

    @Value("${iotdb.fetch_size:50}")
    private int fetchSize;

    @Value("${iotdb.max_size:200}")
    private int maxSize;

    @Value("${iotdb.database:root.thingsboard}")
    private String dbName;

    @PostConstruct
    public void createPool() {
        myIotdbSessionPool = createSessionPool();
    }

    private SessionPool createSessionPool() {
        return new SessionPool.Builder()
                .host(host)
                .port(port)
                .user(user)
                .password(password)
                .connectionTimeoutInMs(timeout)
                .fetchSize(fetchSize)
                .maxSize(maxSize)
                .build();
    }

    @PreDestroy
    public void stop() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }

        myIotdbSessionPool.close();
    }

    public BasicKvEntry getEntry(String key , Field field) {
        if(TSDataType.BOOLEAN.equals(field.getDataType())){
            return new BooleanDataEntry(key,field.getBoolV());
        }
        if(TSDataType.INT64.equals(field.getDataType())){
            return new LongDataEntry(key,field.getLongV());
        }
        if(TSDataType.DOUBLE.equals(field.getDataType())){
            return new DoubleDataEntry(key,field.getDoubleV());
        }
        if(TSDataType.TEXT.equals(field.getDataType())){
            return new StringDataEntry(key,field.getStringValue());
        }
        return new StringDataEntry(key,field.getStringValue());
    }

}
