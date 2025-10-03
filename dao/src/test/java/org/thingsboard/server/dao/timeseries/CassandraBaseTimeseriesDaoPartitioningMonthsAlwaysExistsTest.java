/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateWriteExecutor;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraBaseTimeseriesDao.class)
@TestPropertySource(properties = {
        "database.ts.type=cassandra",
        "cassandra.query.ts_key_value_partitioning=MONTHS",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExistsTest {

    @MockitoSpyBean
    CassandraBaseTimeseriesDao tsDao;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    @Qualifier("CassandraCluster")
    CassandraCluster cassandraCluster;

    @MockBean
    CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;
    @MockBean
    CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Test
    public void testToPartitionsMonths() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("MONTHS");
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(1640995200000L).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime())).isEqualTo(1651363200000L).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:01Z").getTime())).isEqualTo(1651363200000L).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(1651363200000L).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(1701388800000L).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-01T00:00:00Z").getTime());
    }

    @Test
    public void testCalculatePartitionsMonths() throws ParseException {
        long startTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-12T00:00:00Z").getTime());
        long nextTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-31T23:59:59Z").getTime());
        long leapTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-29T23:59:59Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-31T23:59:59Z").getTime());
        log.info("startTs {}, nextTs {}, leapTs {}, endTs {}", startTs, nextTs, leapTs, endTs);

        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(1575158400000L)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-01T00:00:00Z").getTime()));
        assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(1575158400000L, 1577836800000L)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, leapTs)).isEqualTo(List.of(1575158400000L, 1577836800000L, 1580515200000L)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-01T00:00:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(14).isEqualTo(List.of(
                1575158400000L,
                1577836800000L, 1580515200000L, 1583020800000L,
                1585699200000L, 1588291200000L, 1590969600000L,
                1593561600000L, 1596240000000L, 1598918400000L,
                1601510400000L, 1604188800000L, 1606780800000L,
                1609459200000L)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-03-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-04-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-05-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-06-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-07-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-08-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-09-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-10-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-11-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-12-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime()));
    }

    @Test
    public void testEstimatePartitionCount() throws ParseException {
        assertThat(tsDao.estimatePartitionCount(0, Long.MAX_VALUE)).as("centuries").isEqualTo(3_507_324_297L);
        assertThat(tsDao.estimatePartitionCount(0, 0)).as("single").isEqualTo(1L);
        long startTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-12T00:00:00Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-31T23:59:59Z").getTime());
        assertThat(tsDao.estimatePartitionCount(startTs, endTs)).as("13 month + 2 spare periods").isEqualTo(13 + 2);
        assertThat(tsDao.estimatePartitionCount(endTs, startTs)).as("wrong period estimated as 1").isEqualTo(1L);
    }

    @Test
    public void testGetPartitionsFutureModeratePartitionsCount() throws ParseException {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TsKvQuery query = mock(TsKvQuery.class);
        long startTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-12T00:00:00Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-31T23:59:59Z").getTime());

        willReturn(mock(ListenableFuture.class)).given(tsDao).getPartitionsFromDB(tenantId, query, tenantId, startTs, endTs);

        tsDao.getPartitionsFuture(tenantId, query, tenantId, startTs, endTs);

        verify(tsDao).estimatePartitionCount(startTs, endTs);
        verify(tsDao).calculatePartitions(eq(startTs), eq(endTs), anyInt());
        verify(tsDao, never()).getPartitionsFromDB(tenantId, query, tenantId, startTs, endTs);
    }

    @Test
    public void testGetPartitionsFutureHugePartitionsCountPreventOOMFallbackToDB() throws ParseException {

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        TsKvQuery query = mock(TsKvQuery.class);
        long startTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2000-12-12T00:00:00Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("3000-01-31T23:59:59Z").getTime());

        willReturn(mock(ListenableFuture.class)).given(tsDao).getPartitionsFromDB(tenantId, query, tenantId, startTs, endTs);

        tsDao.getPartitionsFuture(tenantId, query, tenantId, startTs, endTs);

        verify(tsDao).estimatePartitionCount(startTs, endTs);
        verify(tsDao, never()).calculatePartitions(eq(startTs), eq(endTs), anyInt());
        verify(tsDao).getPartitionsFromDB(tenantId, query, tenantId, startTs, endTs);
    }

}
