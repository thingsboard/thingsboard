/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.partitions;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateExecutor;
import org.thingsboard.server.dao.timeseries.CassandraBaseTimeseriesDao;
import org.thingsboard.server.dao.timeseries.CassandraPartitionCacheKey;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;

@Slf4j
@RunWith(PowerMockRunner.class)
@PrepareForTest(CassandraBaseTimeseriesDao.class)
public class TestCassandraPartitionsCache {

    private CassandraBaseTimeseriesDao cassandraBaseTimeseriesDao;

    @Mock
    private Environment environment;

    @Mock
    private CassandraBufferedRateExecutor rateLimiter;

    @Mock
    private CassandraCluster cluster;

    @Mock
    private GuavaSession guavaSession;

    @Mock
    private PreparedStatement preparedStatement;

    @Before
    public void setUp() {
        when(cluster.getDefaultReadConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getDefaultReadConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getSession()).thenReturn(guavaSession);
        when(guavaSession.prepare(anyString())).thenReturn(preparedStatement);

        cassandraBaseTimeseriesDao = spy(new CassandraBaseTimeseriesDao());

        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitioning", "MONTHS");
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitionsCacheSize", 100000);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "systemTtl", 0);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "setNullValuesEnabled", false);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "environment", environment);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "rateLimiter", rateLimiter);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "cluster", cluster);
    }

    @Test
    public void testPartitionSave() throws Exception {

        cassandraBaseTimeseriesDao.init();

        doReturn(Futures.immediateFuture(0)).when(cassandraBaseTimeseriesDao,
                "doSavePartition", any(TenantId.class), anyLong(), anyLong(), any(EntityId.class), anyString());


        UUID id = UUID.randomUUID();
        TenantId tenantId = new TenantId(id);
        long tsKvEntryTs = System.currentTimeMillis();

        for (int i = 0; i < 50000; i++) {
            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i, 0);
        }

        for (int i = 0; i < 60000; i++) {
            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i, 0);
        }

        verifyPrivate(cassandraBaseTimeseriesDao, times(60000)).invoke("doSavePartition", any(TenantId.class), anyLong(), anyLong(), any(EntityId.class), anyString());

    }
}
