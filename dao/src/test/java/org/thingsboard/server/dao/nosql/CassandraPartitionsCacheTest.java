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

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.timeseries.CassandraBaseTimeseriesDao;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CassandraPartitionsCacheTest {

    @Spy
    private CassandraBaseTimeseriesDao cassandraBaseTimeseriesDao;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private BoundStatement boundStatement;

    @Mock
    private Environment environment;

    @Mock
    private CassandraBufferedRateExecutor rateLimiter;

    @Mock
    private CassandraCluster cluster;

    @Mock
    private GuavaSession session;

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitioning", "MONTHS");
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitionsCacheSize", 100000);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "systemTtl", 0);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "setNullValuesEnabled", false);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "environment", environment);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "rateLimiter", rateLimiter);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "cluster", cluster);

        when(cluster.getDefaultReadConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getDefaultWriteConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getSession()).thenReturn(session);
        when(session.prepare(anyString())).thenReturn(preparedStatement);

        when(preparedStatement.bind()).thenReturn(boundStatement);

        when(boundStatement.setString(anyInt(), anyString())).thenReturn(boundStatement);
        when(boundStatement.setUuid(anyInt(), any(UUID.class))).thenReturn(boundStatement);
        when(boundStatement.setLong(anyInt(), any(Long.class))).thenReturn(boundStatement);

        doReturn(Futures.immediateFuture(0)).when(cassandraBaseTimeseriesDao).getFuture(any(TbResultSetFuture.class), any());
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