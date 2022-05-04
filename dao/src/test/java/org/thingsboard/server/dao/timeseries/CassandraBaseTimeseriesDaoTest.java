/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
import org.thingsboard.server.dao.nosql.CassandraBufferedRateExecutor;

import java.text.ParseException;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraBaseTimeseriesDao.class)
@TestPropertySource(properties = {
        "database.ts.type=cassandra",
        "cassandra.query.ts_key_value_partitioning=MONTHS",
        "cassandra.query.ts_key_value_partitioning_always_exist_in_reading=true",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoTest {

    @Autowired
    CassandraBaseTimeseriesDao tsDao;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    @Qualifier("CassandraCluster")
    CassandraCluster cassandraCluster;
    @MockBean
    CassandraBufferedRateExecutor cassandraBufferedRateExecutor;

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

        log.warn("startTs {}, nextTs {}, leapTs {}, endTs {}", startTs, nextTs, leapTs, endTs);

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

}
