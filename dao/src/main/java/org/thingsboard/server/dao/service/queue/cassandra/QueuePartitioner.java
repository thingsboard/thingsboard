/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.queue.cassandra;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.service.queue.cassandra.repository.ProcessedPartitionRepository;
import org.thingsboard.server.dao.timeseries.TsPartitionDate;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@NoSqlDao
public class QueuePartitioner {

    private final TsPartitionDate tsFormat;
    private ProcessedPartitionRepository processedPartitionRepository;
    private Clock clock = Clock.systemUTC();

    public QueuePartitioner(@Value("${cassandra.queue.partitioning}") String partitioning,
                            ProcessedPartitionRepository processedPartitionRepository) {
        this.processedPartitionRepository = processedPartitionRepository;
        Optional<TsPartitionDate> partition = TsPartitionDate.parse(partitioning);
        if (partition.isPresent()) {
            tsFormat = partition.get();
        } else {
            log.warn("Incorrect configuration of partitioning {}", partitioning);
            throw new RuntimeException("Failed to parse partitioning property: " + partitioning + "!");
        }
    }

    public long getPartition(long ts) {
        //TODO: use TsPartitionDate.truncateTo?
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return tsFormat.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public List<Long> findUnprocessedPartitions(UUID nodeId, long clusteredHash) {
        Optional<Long> lastPartitionOption = processedPartitionRepository.findLastProcessedPartition(nodeId, clusteredHash);
        long lastPartition = lastPartitionOption.orElse(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7));
        List<Long> unprocessedPartitions = Lists.newArrayList();

        LocalDateTime current = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastPartition), ZoneOffset.UTC);
        LocalDateTime end = LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC)
                .plus(1L, tsFormat.getTruncateUnit());

        while (current.isBefore(end)) {
            current = current.plus(1L, tsFormat.getTruncateUnit());
            unprocessedPartitions.add(tsFormat.truncatedTo(current).toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        return unprocessedPartitions;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void checkProcessedPartitions() {
        //todo-vp: we need to implement this
    }
}
