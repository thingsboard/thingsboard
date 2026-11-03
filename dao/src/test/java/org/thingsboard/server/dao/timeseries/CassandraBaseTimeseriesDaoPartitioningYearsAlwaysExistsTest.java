// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateWriteExecutor;

import java.text.ParseException;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraBaseTimeseriesDao.class)
@TestPropertySource(properties = {
        "database.ts.type=cassandra",
        "cassandra.query.ts_key_value_partitioning=YEARS",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoPartitioningYearsAlwaysExistsTest {

    @Autowired
    CassandraBaseTimeseriesDao tsDao;

    @MockitoBean(answers = Answers.RETURNS_MOCKS)
    @Qualifier("CassandraCluster")
    CassandraCluster cassandraCluster;

    @MockitoBean
    CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;
    @MockitoBean
    CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Test
    public void testToPartitionsYears() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("YEARS");
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:01Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-01-01T00:00:00Z").getTime());
    }

    @Test
    public void testCalculatePartitionsYears() throws ParseException {
        long startTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime());
        long nextTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-10-12T23:59:59Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2025-07-15T00:00:00Z").getTime());
        log.info("startTs {}, nextTs {}, endTs {}", startTs, nextTs, endTs);

        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime()));
        assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(7).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2024-01-01T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2025-01-01T00:00:00Z").getTime()));
    }

    @Test
    public void testEstimatePartitionCount() throws ParseException {
        assertThat(tsDao.estimatePartitionCount(0, Long.MAX_VALUE)).as("centuries").isEqualTo(292_277_026L);
        assertThat(tsDao.estimatePartitionCount(0, 0)).as("single").isEqualTo(1L);
        long startTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-12T00:00:00Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-31T23:59:59Z").getTime());
        assertThat(tsDao.estimatePartitionCount(startTs, endTs)).as("2 years + 2 spare periods").isEqualTo(2 + 2);
        assertThat(tsDao.estimatePartitionCount(endTs, startTs)).as("wrong period estimated as 1").isEqualTo(1L);
    }

}
