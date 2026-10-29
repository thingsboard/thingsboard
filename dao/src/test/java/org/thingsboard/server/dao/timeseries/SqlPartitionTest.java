// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.timeseries;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlPartitionTest {

    @ParameterizedTest
    @EnumSource(value = SqlTsPartitionDate.class, names = {"DAYS", "MONTHS", "YEARS"})
    void quotesExtendedYearPartitionName(SqlTsPartitionDate partitioning) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(4597819750871990276L), ZoneOffset.UTC);
        LocalDateTime start = partitioning.trancateTo(time);
        long startTs = start.toInstant(ZoneOffset.UTC).toEpochMilli();
        long endTs = partitioning.plusTo(start).toInstant(ZoneOffset.UTC).toEpochMilli();
        String date = start.format(DateTimeFormatter.ofPattern(partitioning.getPattern()));

        assertTrue(date.startsWith("+"));
        SqlPartition partition = new SqlPartition(SqlPartition.TS_KV, startTs, endTs, date);

        assertEquals("CREATE TABLE IF NOT EXISTS \"ts_kv_" + date + "\" PARTITION OF ts_kv FOR VALUES FROM ("
                + startTs + ") TO (" + endTs + ")", partition.getQuery());
        assertEquals(startTs, partition.getStart());
        assertEquals(endTs, partition.getEnd());
        assertEquals(date, partition.getPartitionDate());
    }

    @ParameterizedTest
    @CsvSource({
            "2026, ts_kv_2026",
            "2026_10, ts_kv_2026_10",
            "2026_10_02, ts_kv_2026_10_02",
            "-10, ts_kv_-10",
            "with\"quote, ts_kv_with\"\"quote"
    })
    void quotesPartitionIdentifier(String date, String quotedName) {
        SqlPartition partition = new SqlPartition(SqlPartition.TS_KV, 100, 200, date);

        assertEquals("CREATE TABLE IF NOT EXISTS \"" + quotedName
                + "\" PARTITION OF ts_kv FOR VALUES FROM (100) TO (200)", partition.getQuery());
        assertEquals(date, partition.getPartitionDate());
    }
}
