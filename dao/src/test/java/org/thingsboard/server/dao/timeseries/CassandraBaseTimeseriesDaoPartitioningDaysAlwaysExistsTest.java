/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
        "cassandra.query.ts_key_value_partitioning=DAYS",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoPartitioningDaysAlwaysExistsTest {

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
    public void testToPartitionsDays() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("DAYS");
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-02T00:00:00Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-02T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-03T00:00:01Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-03T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T00:00:00Z").getTime());
    }

    @Test
    public void testCalculatePartitionsDays() throws ParseException {
        long startTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime());
        long nextTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-12T23:59:59Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-15T00:00:00Z").getTime());
        log.info("startTs {}, nextTs {}, endTs {}", startTs, nextTs, endTs);

        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime()));
        assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-11T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-12T00:00:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(6).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-11T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-12T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-13T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-14T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-15T00:00:00Z").getTime()));

        long leapStartTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-27T00:00:00Z").getTime());
        long leapEndTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-03-01T00:00:00Z").getTime());
        assertThat(tsDao.calculatePartitions(leapStartTs, leapEndTs)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-27T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-28T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-29T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-03-01T00:00:00Z").getTime()));

        long newYearStartTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-12-30T00:00:00Z").getTime());
        long newYearEndTs = tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime());
        assertThat(tsDao.calculatePartitions(newYearStartTs, newYearEndTs)).isEqualTo(List.of(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-12-30T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2020-12-31T00:00:00Z").getTime(),
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime()));
    }

}
