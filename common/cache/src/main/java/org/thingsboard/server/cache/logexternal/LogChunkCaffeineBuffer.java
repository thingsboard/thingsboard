// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.logexternal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@Service("LogChunkBuffer")
public class LogChunkCaffeineBuffer implements LogChunkBuffer {

    private static final int CHUNK_OVERHEAD_BYTES = 64;
    private static final int LINE_OVERHEAD_BYTES = 48;

    private final Cache<UnitKey, UnitBuffer> buffers;
    private final int maxChunksPerUnit;

    public LogChunkCaffeineBuffer(@Value("${agents.logStream.bufferTtlSeconds:900}") long ttlSeconds,
                                  @Value("${agents.logStream.bufferMaxChunksPerUnit:10}") int maxChunksPerUnit,
                                  @Value("${agents.logStream.bufferMaxBytes:67108864}") long maxBytes) {
        this.buffers = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumWeight(maxBytes)
                .<UnitKey, UnitBuffer>weigher((key, buffer) -> buffer.approxBytes())
                .build();
        this.maxChunksPerUnit = maxChunksPerUnit;
    }

    @Override
    public long append(TenantId tenantId, EntityId unitId, LogChunk chunk) {
        UnitKey key = new UnitKey(tenantId.getId(), unitId.getId());
        AtomicLong assignedSeq = new AtomicLong();
        buffers.asMap().compute(key, (k, existing) -> {
            UnitBuffer unit = existing != null ? existing : new UnitBuffer();
            long seq = unit.seq().incrementAndGet();
            assignedSeq.set(seq);
            chunk.setSeq(seq);
            unit.chunks().put(seq, chunk);
            while (unit.chunks().size() > maxChunksPerUnit) {
                unit.chunks().pollFirstEntry();
            }
            return unit;
        });
        return assignedSeq.get();
    }

    @Override
    public List<LogChunk> range(TenantId tenantId, EntityId unitId, long fromSeqInclusive, long toSeqInclusive) {
        if (toSeqInclusive < fromSeqInclusive) {
            return List.of();
        }
        UnitBuffer unit = buffers.getIfPresent(new UnitKey(tenantId.getId(), unitId.getId()));
        if (unit == null) {
            return List.of();
        }
        return new ArrayList<>(unit.chunks().subMap(fromSeqInclusive, true, toSeqInclusive, true).values());
    }

    @Override
    public long latestTailSeq(TenantId tenantId, EntityId unitId) {
        UnitBuffer unit = buffers.getIfPresent(new UnitKey(tenantId.getId(), unitId.getId()));
        return (unit == null || unit.chunks().isEmpty()) ? 0 : unit.chunks().lastKey();
    }

    @Override
    public long latestTailLineTs(TenantId tenantId, EntityId unitId) {
        UnitBuffer unit = buffers.getIfPresent(new UnitKey(tenantId.getId(), unitId.getId()));
        if (unit == null || unit.chunks().isEmpty()) {
            return 0;
        }
        return unit.chunks().lastEntry().getValue().getLastLineTs();
    }

    @Override
    public void deleteUnit(TenantId tenantId, EntityId unitId) {
        buffers.invalidate(new UnitKey(tenantId.getId(), unitId.getId()));
    }

    private record UnitKey(UUID tenantId, UUID unitId) {}

    private record UnitBuffer(AtomicLong seq, NavigableMap<Long, LogChunk> chunks) {
        UnitBuffer() {
            this(new AtomicLong(), new ConcurrentSkipListMap<>());
        }

        int approxBytes() {
            long bytes = 0;
            for (LogChunk chunk : chunks.values()) {
                bytes += CHUNK_OVERHEAD_BYTES;
                List<String> lines = chunk.getLines();
                if (lines != null) {
                    for (String line : lines) {
                        bytes += LINE_OVERHEAD_BYTES + (line == null ? 0 : line.length() * 2L);
                    }
                }
            }
            return (int) Math.min(bytes, Integer.MAX_VALUE);
        }
    }

}
