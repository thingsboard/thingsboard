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

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CassandraPartitionsCacheTest {

//    private CassandraBaseTimeseriesDao cassandraBaseTimeseriesDao;
//
//    @Mock
//    private Environment environment;
//
//    @Mock
//    private CassandraBufferedRateExecutor rateLimiter;
//
//    @Mock
//    private CassandraCluster cluster;
//
//    @Mock
//    private Session session;
//
//    @Mock
//    private Cluster sessionCluster;
//
//    @Mock
//    private Configuration configuration;
//
//    @Mock
//    private PreparedStatement preparedStatement;
//
//    @Mock
//    private BoundStatement boundStatement;
//
//    @Before
//    public void setUp() {
//        when(cluster.getDefaultReadConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
//        when(cluster.getDefaultWriteConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
//        when(cluster.getSession()).thenReturn(session);
//        when(session.getCluster()).thenReturn(sessionCluster);
//        when(sessionCluster.getConfiguration()).thenReturn(configuration);
//        when(configuration.getCodecRegistry()).thenReturn(CodecRegistry.DEFAULT_INSTANCE);
//        when(session.prepare(anyString())).thenReturn(preparedStatement);
//        when(preparedStatement.bind()).thenReturn(boundStatement);
//        when(boundStatement.setString(anyInt(), anyString())).thenReturn(boundStatement);
//        when(boundStatement.setUUID(anyInt(), any(UUID.class))).thenReturn(boundStatement);
//        when(boundStatement.setLong(anyInt(), anyLong())).thenReturn(boundStatement);
//        when(boundStatement.setInt(anyInt(), anyInt())).thenReturn(boundStatement);
//
//        cassandraBaseTimeseriesDao = spy(new CassandraBaseTimeseriesDao());
//
//        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitioning", "MONTHS");
//        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitionsCacheSize", 100000);
//        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "systemTtl", 0);
//        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "setNullValuesEnabled", false);
//        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "environment", environment);
//        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "rateLimiter", rateLimiter);
//        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "cluster", cluster);
//
//        doReturn(Futures.immediateFuture(null)).when(cassandraBaseTimeseriesDao).getFuture(any(ResultSetFuture.class), any());
//
//    }
//
//    @Test
//    public void testPartitionSave() throws Exception {
//
//        cassandraBaseTimeseriesDao.init();
//
//
//        UUID id = UUID.randomUUID();
//        TenantId tenantId = new TenantId(id);
//        long tsKvEntryTs = System.currentTimeMillis();
//
//        for (int i = 0; i < 50000; i++) {
//            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i, 0);
//        }
//
//        for (int i = 0; i < 60000; i++) {
//            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i, 0);
//        }
//
//        verify(cassandraBaseTimeseriesDao, times(60000)).executeAsyncWrite(any(TenantId.class), any(Statement.class));
//
//
//    }

}