/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.timeseries;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateWriteExecutor;

import java.text.ParseException;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraBaseTimeseriesDao.class)
@TestPropertySource(properties = {
        "database.ts.type=cassandra",
        "cassandra.query.ts_key_value_partitioning=MONTHS",
        "cassandra.query.ts_key_value_partitioning_always_exist_in_reading=true",
        "cassandra.query.ts_key_value_partitioning_write_partition_table=true",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoTest {

    @SpyBean
    CassandraBaseTimeseriesDao tsDao;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    @Qualifier("CassandraCluster")
    CassandraCluster cassandraCluster;
    @MockBean
    CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;
    @MockBean
    CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Test
    public void testPartitionCache() {
        assertThat(tsDao.cassandraTsPartitionsCache).isNotNull();
    }

    @Test
    public void testSavePartition() {
        ListenableFuture<Integer> future = Futures.submit(() -> 0, MoreExecutors.directExecutor());
        willReturn(future).given(tsDao).doSavePartition(any(), any(), anyString(), anyLong(), anyLong());
        assertThat(tsDao.savePartition(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, 0L, "key")).isSameAs(future);
    }

    @Test
    public void testToPartitionsMonths() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("MONTHS");
        assertThat(tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(1640995200000L);
        assertThat(tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime())).isEqualTo(1651363200000L);
        assertThat(tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:01Z").getTime())).isEqualTo(1651363200000L);
        assertThat(tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(1651363200000L);
        assertThat(tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(1701388800000L);
    }

    @Test
    public void testCalculatePartitions() throws ParseException {
        long startTs = tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-12T00:00:00Z").getTime());
        long nextTs = tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-31T23:59:59Z").getTime());
        long leapTs = tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-29T23:59:59Z").getTime());
        long endTs = tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-31T23:59:59Z").getTime());

        log.debug("startTs {}, nextTs {}, leapTs {}, endTs {}", startTs, nextTs, leapTs, endTs);

        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));
        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(1575158400000L));
        assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(1575158400000L, 1577836800000L));
        assertThat(tsDao.calculatePartitions(startTs, leapTs)).isEqualTo(List.of(1575158400000L, 1577836800000L, 1580515200000L));

        assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(14);
        assertThat(tsDao.calculatePartitions(startTs, endTs)).isEqualTo(List.of(
                1575158400000L,
                1577836800000L, 1580515200000L, 1583020800000L,
                1585699200000L, 1588291200000L, 1590969600000L,
                1593561600000L, 1596240000000L, 1598918400000L,
                1601510400000L, 1604188800000L, 1606780800000L,
                1609459200000L));
    }

    @Test
    public void givenPartitioning_whenInit_thenPartitionMaxMsSet() {
        assertThat(tsDao.partition_max_ms).isEqualTo(31 * 24 * 60 * 60 * 1000L);
    }

    @Test
    public void givenPartitioning_whenCalculatePartitioningMaxTs_thenReturnMs() {
        assertThat(tsDao.calculatePartitionMaxMs("MINUTES")).as("MINUTES").isEqualTo(60 * 1000L);
        assertThat(tsDao.calculatePartitionMaxMs("HOURS")).as("HOURS").isEqualTo(60 * 60 * 1000L);
        assertThat(tsDao.calculatePartitionMaxMs("DAYS")).as("DAYS").isEqualTo(24 * 60 * 60 * 1000L);
        assertThat(tsDao.calculatePartitionMaxMs("MONTHS")).as("MONTHS").isEqualTo(31 * 24 * 60 * 60 * 1000L);
        assertThat(tsDao.calculatePartitionMaxMs("YEARS")).as("YEARS").isEqualTo(366 * 24 * 60 * 60 * 1000L);
        assertThat(tsDao.calculatePartitionMaxMs("INDEFINITE")).as("INDEFINITE").isEqualTo(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void givenPartitioningUnknown_whenCalculatePartitioningMaxTs_thenException() {
        tsDao.calculatePartitionMaxMs("LARGE");
    }

}
