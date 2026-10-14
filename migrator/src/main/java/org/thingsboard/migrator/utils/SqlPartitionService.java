// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.utils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.Table;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@ConfigurationProperties
@Slf4j
public class SqlPartitionService {

    private final JdbcTemplate jdbcTemplate;
    @Setter
    private Map<String, Integer> partitionSizes;

    private final Map<Table, Set<Long>> partitions = new HashMap<>();

    public void createPartition(Table table, Map<String, Object> row) {
        long partitionSize = getPartitionSize(table);
        long ts = (long) row.get(table.getPartitionColumn());
        long partitionStart = ts - (ts % partitionSize);
        long partitionEnd = partitionStart + partitionSize;

        boolean newPartition = partitions.computeIfAbsent(table, t -> new HashSet<>()).add(partitionStart);
        if (newPartition) {
            String query = format("CREATE TABLE IF NOT EXISTS %s_%s PARTITION OF %s FOR VALUES FROM (%s) TO (%s)",
                    table.getName(), partitionStart, table.getName(), partitionStart, partitionEnd);
            log.info("Created partition for table {} ({}-{})", table.getName(), partitionStart, partitionEnd);
            jdbcTemplate.execute(query);
        }
    }

    public Map<Long, Long> getPartitions(Table table) {
        long partitionSize = getPartitionSize(table);
        return jdbcTemplate.queryForList("SELECT tablename FROM pg_tables " +
                                         "WHERE tablename LIKE '" + table.getName() + "_%'", String.class).stream()
                .map(partition -> StringUtils.substringAfterLast(partition, "_"))
                .filter(NumberUtils::isParsable)
                .map(Long::parseLong)
                .collect(Collectors.toMap(startTs -> startTs, startTs -> startTs + partitionSize));
    }

    private long getPartitionSize(Table table) {
        return TimeUnit.HOURS.toMillis(partitionSizes.get(table.getPartitionSizeSettingsKey()));
    }

}
