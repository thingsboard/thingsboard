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
package org.thingsboard.server.dao.nosql;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.Configuration;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.timeseries.CassandraBaseTimeseriesDao;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CassandraPartitionsCacheTest {

    private CassandraBaseTimeseriesDao cassandraBaseTimeseriesDao;

    @Mock
    private Environment environment;

    @Mock
    private CassandraBufferedRateExecutor rateLimiter;

    @Mock
    private CassandraCluster cluster;

    @Mock
    private Session session;

    @Mock
    private Cluster sessionCluster;

    @Mock
    private Configuration configuration;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private BoundStatement boundStatement;

    @Before
    public void setUp() {
        when(cluster.getDefaultReadConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getDefaultWriteConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getSession()).thenReturn(session);
        when(session.getCluster()).thenReturn(sessionCluster);
        when(sessionCluster.getConfiguration()).thenReturn(configuration);
        when(configuration.getCodecRegistry()).thenReturn(CodecRegistry.DEFAULT_INSTANCE);
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind()).thenReturn(boundStatement);
        when(boundStatement.setString(anyInt(), anyString())).thenReturn(boundStatement);
        when(boundStatement.setUUID(anyInt(), any(UUID.class))).thenReturn(boundStatement);
        when(boundStatement.setLong(anyInt(), anyLong())).thenReturn(boundStatement);
        when(boundStatement.setInt(anyInt(), anyInt())).thenReturn(boundStatement);

        cassandraBaseTimeseriesDao = spy(new CassandraBaseTimeseriesDao());

        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitioning", "MONTHS");
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitionsCacheSize", 100000);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "systemTtl", 0);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "setNullValuesEnabled", false);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "environment", environment);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "rateLimiter", rateLimiter);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "cluster", cluster);

        doReturn(Futures.immediateFuture(null)).when(cassandraBaseTimeseriesDao).getFuture(any(ResultSetFuture.class), any());

    }

    @Test
    public void testPartitionSave() throws Exception {

        cassandraBaseTimeseriesDao.init();


        UUID id = UUID.randomUUID();
        TenantId tenantId = new TenantId(id);
        long tsKvEntryTs = System.currentTimeMillis();

        for (int i = 0; i < 50000; i++) {
            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i, 0);
        }

        for (int i = 0; i < 60000; i++) {
            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i, 0);
        }

        verify(cassandraBaseTimeseriesDao, times(60000)).executeAsyncWrite(any(TenantId.class), any(Statement.class));


    }

}