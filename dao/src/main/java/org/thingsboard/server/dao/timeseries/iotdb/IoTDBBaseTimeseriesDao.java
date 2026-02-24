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
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.util.IoTDBAnyDao;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Component
@Configuration
@Data
@IoTDBAnyDao
public class IoTDBBaseTimeseriesDao {

    protected ExecutorService readProcessingExecutor;
    protected ExecutorService saveProcessingExecutor;

    protected SessionPool iotdbSessionPool;

    @Value("${iotdb.nodeUrls:127.0.0.1:6667}")
    private List<String> nodeUrls;

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

    @Value("${iotdb.result_process_threads:50}")
    private int resultThreadPoolSize;

    @Value("${iotdb.save_process_threads:50}")
    private int saveThreadPoolSize;

    @Value("${iotdb.enable_batch:true}")
    private boolean enableBatch;

    @Value("${iotdb.batch_size:1000}")
    private int batchSize;

    @Value("${iotdb.batch_max_delay:100}")
    private int maxDelay;

    @Value("${iotdb.stats_print_interval_ms:5000}")
    private int statsPrintIntervalMs;

    @Value("${iotdb.enable_latest_batch:true}")
    private boolean enableLatestBatch;

    @Value("${iotdb.latest_batch_size:1000}")
    private int latestBatchSize;

    @Value("${iotdb.latest_batch_max_delay:100}")
    private int latestMaxDelay;

    @Value("${iotdb.latest_stats_print_interval_ms:5000}")
    private int latestStatsPrintIntervalMs;


    @PostConstruct
    public void startExecutor() {
        readProcessingExecutor = ThingsBoardExecutors.newWorkStealingPool(resultThreadPoolSize, "iotdb-read-callback");
        saveProcessingExecutor = ThingsBoardExecutors.newWorkStealingPool(saveThreadPoolSize, "iotdb-save-callback");
    }
    @PostConstruct
    public void createPool() {
        iotdbSessionPool = createSessionPool();
    }

    @PreDestroy
    public void stopExecutor() {
        if (readProcessingExecutor != null) {
            readProcessingExecutor.shutdownNow();
        }
        if(saveProcessingExecutor != null) {
            saveProcessingExecutor.shutdownNow();
        }
        iotdbSessionPool.close();
    }
    private SessionPool createSessionPool() {
        return new SessionPool.Builder()
                .nodeUrls(nodeUrls)
                .user(user)
                .password(password)
                .connectionTimeoutInMs(timeout)
                .fetchSize(fetchSize)
                .maxSize(maxSize)
                .build();
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
