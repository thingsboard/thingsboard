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

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
        "cassandra.query.ts_key_value_partitioning=MINUTES",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoPartitioningMinutesAlwaysExistsTest {

    @Autowired
    CassandraBaseTimeseriesDao tsDao;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    @Qualifier("CassandraCluster")
    CassandraCluster cassandraCluster;

    @MockBean
    CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;
    @MockBean
    CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Test
    public void testToPartitionsMinutes() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("MINUTES");
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-02T00:01:00Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-02T00:01:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-03T00:02:01Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-03T00:02:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:00Z").getTime());
    }


    @Test
    public void testCalculatePartitionsMinutes() throws ParseException {
        long startTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime());
        long nextTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:02:59Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:10:00Z").getTime());
        log.info("startTs {}, nextTs {}, endTs {}", startTs, nextTs, endTs);

        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime()));
        assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:01:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:02:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(11).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:01:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:02:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:03:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:04:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:05:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:06:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:07:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:08:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:09:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:10:00Z").getTime()));
    }

    @Test
    public void testEstimatePartitionCount() throws ParseException {
        assertThat(tsDao.estimatePartitionCount(0, Long.MAX_VALUE)).as("centuries").isEqualTo(153_722_867_280_914L);
        assertThat(tsDao.estimatePartitionCount(0, 0)).as("single").isEqualTo(1L);
        long startTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-12T00:00:00Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-31T23:59:59Z").getTime());
        assertThat(tsDao.estimatePartitionCount(startTs, endTs)).as("600,479 minutes + 2 spare periods").isEqualTo(600479 + 2);
        assertThat(tsDao.estimatePartitionCount(endTs, startTs)).as("wrong period estimated as 1").isEqualTo(1L);
    }

}
