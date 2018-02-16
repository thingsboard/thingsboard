package org.thingsboard.rule.engine.queue.cassandra.repository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedPartitionRepository {

    void partitionProcessed(UUID nodeId, long clusteredHash, long partition);

    Optional<Long> findLastProcessedPartition(UUID nodeId, long clusteredHash);

}
