// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.log;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.gen.transport.TransportProtos.TbLogStreamUpdateProto;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.log.sub.LogsSubscriptionUpdate;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@TbCoreComponent
@Component
public class LogStreamDispatcher {

    /**
     * Caps initial backlog replayed per new subscriber, to bound the per-subscription
     * heap/deserialization burst of materializing the range; older chunks still reach the live path.
     */
    private static final long MAX_TAIL_REPLAY_CHUNKS_LIMIT = 5;

    private final LogChunkBuffer logChunkBuffer;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final StatsFactory statsFactory;
    private final long maxTailReplayChunks;
    private final int dispatcherPoolSize;
    private final int dispatcherQueueSize;
    private ExecutorService deliveryExecutor;
    private DefaultCounter droppedWatermarks;

    private final Map<EntityId, LogStreamState> states = new ConcurrentReferenceHashMap<>(16, ReferenceType.SOFT);

    public LogStreamDispatcher(LogChunkBuffer logChunkBuffer, @Lazy TbLocalSubscriptionService localSubscriptionService,
                               StatsFactory statsFactory,
                               @Value("${agents.logStream.bufferMaxChunksPerUnit:10}") int bufferMaxChunksPerUnit,
                               @Value("${agents.logStream.dispatcherPoolSize:8}") int dispatcherPoolSize,
                               @Value("${agents.logStream.dispatcherQueueSize:500}") int dispatcherQueueSize) {
        this.logChunkBuffer = logChunkBuffer;
        this.localSubscriptionService = localSubscriptionService;
        this.statsFactory = statsFactory;
        this.maxTailReplayChunks = Math.max(1, Math.min(MAX_TAIL_REPLAY_CHUNKS_LIMIT, bufferMaxChunksPerUnit));
        this.dispatcherPoolSize = dispatcherPoolSize;
        this.dispatcherQueueSize = dispatcherQueueSize;
    }

    @PostConstruct
    public void init() {
        if (deliveryExecutor == null) {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(dispatcherPoolSize, dispatcherPoolSize,
                    60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(dispatcherQueueSize),
                    ThingsBoardThreadFactory.forName("log-stream-dispatcher"));
            executor.allowCoreThreadTimeOut(true);
            deliveryExecutor = executor;
            droppedWatermarks = statsFactory.createDefaultCounter("logStreamDroppedWatermarks");
            statsFactory.createGauge("logStream", "deliveryQueueSize", executor.getQueue(), Collection::size);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (deliveryExecutor != null) {
            deliveryExecutor.shutdownNow();
        }
    }

    public void onWatermark(TbLogStreamUpdateProto proto, TbCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
        onWatermark(tenantId, entityId, proto.getLatestSeq(), callback);
    }

    public void onWatermark(TenantId tenantId, EntityId entityId, long latestSeq, TbCallback callback) {
        try {
            deliveryExecutor.execute(() -> doOnWatermark(tenantId, entityId, latestSeq, callback));
        } catch (RejectedExecutionException e) {
            droppedWatermarks.increment();
            log.warn("[{}][{}] Log stream delivery queue is full, dropping watermark {}", tenantId, entityId, latestSeq);
            callback.onSuccess();
        }
    }

    private void doOnWatermark(TenantId tenantId, EntityId entityId, long latestSeq, TbCallback callback) {
        LogStreamState state = getState(tenantId, entityId);
        // unlocked fast path for the common duplicate watermark; anything BELOW the watermark falls through to the
        // locked block, which consults the tail and can tell a reordered watermark from a rewound buffer
        if (latestSeq == state.lastSeenSeq) {
            callback.onSuccess();
            return;
        }
        try {
            synchronized (state) {
                if (latestSeq <= state.lastSeenSeq) {
                    long actualTail = logChunkBuffer.latestTailSeq(tenantId, entityId);
                    if (actualTail >= state.lastSeenSeq) { // genuine stale/reordered watermark
                        callback.onSuccess();
                        return;
                    }
                    state.lastSeenSeq = 0; // buffer rewound -> resync, fall through to deliver
                }
                long previousSeq = state.lastSeenSeq;
                long fromSeq = previousSeq + 1;
                state.lastSeenSeq = latestSeq;
                try {
                    List<LogChunk> chunks = logChunkBuffer.range(tenantId, entityId, fromSeq, latestSeq);
                    int evicted = fromSeq > 1 ? (int) (latestSeq - fromSeq + 1) - chunks.size() : 0;
                    if (chunks.isEmpty() && evicted == 0) {
                        callback.onSuccess();
                        return;
                    }
                    localSubscriptionService.onLogsUpdate(entityId, toUpdate(latestSeq, chunks, evicted), callback);
                } catch (Exception e) {
                    state.lastSeenSeq = previousSeq;
                    throw e;
                }
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to deliver log stream update for seq {}", tenantId, entityId, latestSeq, e);
            callback.onFailure(e);
        }
    }

    public void streamLogTail(TenantId tenantId, EntityId entityId, long fromSeqExclusive, Consumer<LogsSubscriptionUpdate> consumer) {
        streamLogTail(tenantId, entityId, fromSeqExclusive, consumer, () -> { });
    }

    /**
     * Replays the retained log tail to {@code consumer} as a one-time snapshot and then runs {@code registerLiveSub}.
     * The snapshot is bounded by the current per-entity watermark ({@code lastSeenSeq}); both steps run under the
     * per-entity monitor the live path uses, so a watermark can neither re-deliver a chunk the snapshot already
     * carried nor land in the window between the snapshot and the registration.
     * Chunks older than {@code maxTailReplayChunks} are not replayed; the count of those is reported as
     * {@code evictedChunks} on the first emitted update so the client can tell the gap from a quiet stream.
     */
    public void streamLogTail(TenantId tenantId, EntityId entityId, long fromSeqExclusive,
                              Consumer<LogsSubscriptionUpdate> consumer, Runnable registerLiveSub) {
        LogStreamState state = getState(tenantId, entityId);
        synchronized (state) {
            try {
                replayTail(tenantId, entityId, fromSeqExclusive, consumer, state.lastSeenSeq);
            } finally {
                registerLiveSub.run();
            }
        }
    }

    private void replayTail(TenantId tenantId, EntityId entityId, long fromSeqExclusive,
                            Consumer<LogsSubscriptionUpdate> consumer, long toSeq) {
        if (toSeq <= 0 || fromSeqExclusive >= toSeq) {
            return;
        }
        long fromExclusive = Math.max(toSeq - maxTailReplayChunks, fromSeqExclusive);
        int notReplayedChunks = (int) (fromExclusive - fromSeqExclusive);
        List<LogChunk> chunks = logChunkBuffer.range(tenantId, entityId, fromExclusive + 1, toSeq);

        if (chunks.isEmpty()) {
            if (notReplayedChunks > 0) {
                consumer.accept(new LogsSubscriptionUpdate(toSeq, List.of(), 0, notReplayedChunks));
            }
            return;
        }
        for (int i = 0; i < chunks.size(); i++) {
            boolean firstChunk = i == 0;
            LogChunk chunk = chunks.get(i);
            consumer.accept(toUpdate(chunk.getSeq(), chunk, firstChunk ? notReplayedChunks : 0));
        }
    }

    private LogStreamState getState(TenantId tenantId, EntityId entityId) {
        return states.computeIfAbsent(entityId, e -> getInitialLogStreamState(tenantId, e));
    }

    private LogStreamState getInitialLogStreamState(TenantId tenantId, EntityId entityId) {
        long tailSeq = logChunkBuffer.latestTailSeq(tenantId, entityId);
        return new LogStreamState(tailSeq);
    }

    private LogsSubscriptionUpdate toUpdate(long latestSeq, List<LogChunk> chunks, int evictedChunks) {
        List<String> lines = new ArrayList<>();
        int droppedLines = 0;
        for (LogChunk chunk : chunks) {
            if (chunk.getDroppedLines() > 0) {
                droppedLines += chunk.getDroppedLines();
            }
            if (chunk.getLines() != null) {
                lines.addAll(chunk.getLines());
            }
        }
        return new LogsSubscriptionUpdate(latestSeq, lines, droppedLines, evictedChunks);
    }

    private LogsSubscriptionUpdate toUpdate(long latestSeq, LogChunk chunk, int evictedChunks) {
        List<String> lines = chunk.getLines() != null ? chunk.getLines() : List.of();
        return new LogsSubscriptionUpdate(latestSeq, lines, chunk.getDroppedLines(), evictedChunks);
    }

    @AllArgsConstructor
    private static class LogStreamState {
        volatile long lastSeenSeq;
    }

}
