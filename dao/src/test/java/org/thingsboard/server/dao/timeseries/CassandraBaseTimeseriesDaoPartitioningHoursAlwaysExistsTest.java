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
        "cassandra.query.ts_key_value_partitioning=HOURS",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoPartitioningHoursAlwaysExistsTest {

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
    public void testToPartitionsHours() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("HOURS");
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-02T01:00:00Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-02T01:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-03T02:00:01Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-03T02:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:00:00Z").getTime());
    }

    @Test
    public void testCalculatePartitionsHours() throws ParseException {
            long startTs = tsDao.toPartitionTs(
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime());
            long nextTs = tsDao.toPartitionTs(
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T03:59:59Z").getTime());
            long endTs = tsDao.toPartitionTs(
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-11T00:59:00Z").getTime());
            log.info("startTs {}, nextTs {}, endTs {}", startTs, nextTs, endTs);

            assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
            assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

            assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime()));
            assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T01:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T02:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T03:00:00Z").getTime()));

            assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(25).isEqualTo(List.of(
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T01:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T02:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T03:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T04:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T05:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T06:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T07:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T08:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T09:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T10:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T11:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T12:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T13:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T14:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T15:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T16:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T17:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T18:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T19:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T20:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T21:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T22:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T23:00:00Z").getTime(),
                    ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-11T00:00:00Z").getTime()));
    }

}
