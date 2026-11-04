// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.log;

import com.google.common.util.concurrent.MoreExecutors;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.gen.transport.TransportProtos.TbLogStreamUpdateProto;
import org.thingsboard.server.service.log.sub.LogsSubscriptionUpdate;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogStreamDispatcherTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final EntityId ENTITY_ID = new AgentAppUnitId(UUID.randomUUID());

    @Mock
    private LogChunkBuffer logChunkBuffer;
    @Mock
    private TbLocalSubscriptionService localSubscriptionService;
    @Mock
    private StatsFactory statsFactory;

    private LogStreamDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new LogStreamDispatcher(logChunkBuffer, localSubscriptionService, statsFactory, 10, 8, 500);
        ReflectionTestUtils.setField(dispatcher, "deliveryExecutor", MoreExecutors.newDirectExecutorService());
    }

    @Test
    void emitsChunkRangeAsSingleUpdate() {
        LogChunk chunkA = chunkWithLines("A", 3);
        LogChunk chunkB = chunkWithLines("B", 2);

        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 2L)).thenReturn(List.of(chunkA, chunkB));

        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 2L, TbCallback.EMPTY);

        ArgumentCaptor<LogsSubscriptionUpdate> captor = ArgumentCaptor.forClass(LogsSubscriptionUpdate.class);
        verify(localSubscriptionService).onLogsUpdate(eq(ENTITY_ID), captor.capture(), any(TbCallback.class));

        LogsSubscriptionUpdate update = captor.getValue();
        assertThat(update.getLatestSeq()).isEqualTo(2L);
        assertThat(update.getLines()).containsExactly("A0", "A1", "A2", "B0", "B1");
        assertThat(update.getDroppedLines()).isZero();
        assertThat(update.getEvictedChunks()).isZero();
    }

    @Test
    void aggregatesDroppedAcrossChunks() {
        LogChunk dropped = new LogChunk();
        dropped.setLines(List.of());
        dropped.setDroppedLines(7);

        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 1L)).thenReturn(List.of(dropped));

        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 1L, TbCallback.EMPTY);

        ArgumentCaptor<LogsSubscriptionUpdate> captor = ArgumentCaptor.forClass(LogsSubscriptionUpdate.class);
        verify(localSubscriptionService).onLogsUpdate(eq(ENTITY_ID), captor.capture(), any(TbCallback.class));

        assertThat(captor.getValue().getLines()).isEmpty();
        assertThat(captor.getValue().getDroppedLines()).isEqualTo(7);
        assertThat(captor.getValue().getEvictedChunks()).isZero();
        assertThat(captor.getValue().getLatestSeq()).isEqualTo(1L);
    }

    @Test
    void reportsEvictedChunksSeparatelyFromDroppedLines() {
        LogChunk first = chunkWithLines("a", 2);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 2L)).thenReturn(List.of(first));
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 2L, TbCallback.EMPTY);

        LogChunk later = chunkWithLines("b", 1);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 3L, 5L)).thenReturn(List.of(later));
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 5L, TbCallback.EMPTY);

        ArgumentCaptor<LogsSubscriptionUpdate> captor = ArgumentCaptor.forClass(LogsSubscriptionUpdate.class);
        verify(localSubscriptionService, org.mockito.Mockito.times(2))
                .onLogsUpdate(eq(ENTITY_ID), captor.capture(), any(TbCallback.class));

        LogsSubscriptionUpdate second = captor.getAllValues().get(1);
        assertThat(second.getEvictedChunks()).isEqualTo(2);
        assertThat(second.getDroppedLines()).isZero();
    }

    @Test
    void noUpdateWhenSeqAlreadySeen() {
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 5L)).thenReturn(List.of(chunkWithLines("x", 1)));

        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 5L, TbCallback.EMPTY);
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 5L, TbCallback.EMPTY);

        verify(localSubscriptionService).onLogsUpdate(eq(ENTITY_ID), any(), any(TbCallback.class));
    }

    @Test
    void streamLogTailEmitsNothingWhenTailEmpty() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(0L);

        List<LogsSubscriptionUpdate> collected = new java.util.ArrayList<>();
        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 0L, collected::add);

        assertThat(collected).isEmpty();
    }

    @Test
    void streamLogTailStampsEachReplayedChunkWithItsOwnSeq() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(10L);

        LogChunk a = chunkWithLines("a", 2);
        a.setSeq(9L);
        LogChunk b = chunkWithLines("b", 3);
        b.setSeq(10L);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 6L, 10L)).thenReturn(List.of(a, b));

        List<LogsSubscriptionUpdate> collected = new java.util.ArrayList<>();
        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 0L, collected::add);

        assertThat(collected).hasSize(2);
        assertThat(collected.get(0).getLines()).containsExactly("a0", "a1");
        assertThat(collected.get(0).getLatestSeq()).isEqualTo(9L);
        assertThat(collected.get(1).getLines()).containsExactly("b0", "b1", "b2");
        assertThat(collected.get(1).getLatestSeq()).isEqualTo(10L);
    }

    @Test
    void streamLogTailReportsTheNotReplayedChunksOnTheFirstUpdateOnly() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(100L);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 96L, 100L))
                .thenReturn(List.of(chunkWithSeq("a", 99L), chunkWithSeq("b", 100L)));

        List<LogsSubscriptionUpdate> collected = new java.util.ArrayList<>();
        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 3L, collected::add);

        assertThat(collected).hasSize(2);
        assertThat(collected.get(0).getEvictedChunks()).isEqualTo(92);
        assertThat(collected.get(1).getEvictedChunks()).isZero();
    }

    @Test
    void streamLogTailReportsTheGapEvenWhenNothingIsRetained() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(100L);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 96L, 100L)).thenReturn(List.of());

        List<LogsSubscriptionUpdate> collected = new java.util.ArrayList<>();
        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 3L, collected::add);

        assertThat(collected).hasSize(1);
        assertThat(collected.get(0).getLines()).isEmpty();
        assertThat(collected.get(0).getEvictedChunks()).isEqualTo(92);
    }

    @Test
    void streamLogTailRegistersTheLiveSubscriptionAfterTheSnapshot() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(2L);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 2L)).thenReturn(List.of(chunkWithSeq("a", 2L)));

        List<String> order = new java.util.ArrayList<>();
        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 0L, u -> order.add("snapshot"), () -> order.add("register"));

        assertThat(order).containsExactly("snapshot", "register");
    }

    @Test
    void rewoundBufferResyncsInsteadOfAckingStaleWatermarksForever() {
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 5L)).thenReturn(List.of(chunkWithLines("x", 1)));
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 5L, TbCallback.EMPTY);

        // the per-unit seq key expired, so the buffer restarted numbering at 1
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(1L);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 1L)).thenReturn(List.of(chunkWithLines("y", 1)));
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 1L, TbCallback.EMPTY);

        verify(localSubscriptionService, org.mockito.Mockito.times(2))
                .onLogsUpdate(eq(ENTITY_ID), any(), any(TbCallback.class));
    }

    @Test
    void streamLogTailSkipsChunksUpToClientWatermark() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(10L);

        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 7L, u -> { });

        verify(logChunkBuffer).range(TENANT_ID, ENTITY_ID, 8L, 10L);
    }

    @Test
    void streamLogTailEmitsNothingWhenClientWatermarkAlreadyCaughtUp() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(10L);

        List<LogsSubscriptionUpdate> collected = new java.util.ArrayList<>();
        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 10L, collected::add);

        assertThat(collected).isEmpty();
        verify(logChunkBuffer, org.mockito.Mockito.never())
                .range(any(), any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void watermarkAfterTheSnapshotDeliversOnlyWhatTheSnapshotDidNotCarry() {
        when(logChunkBuffer.latestTailSeq(TENANT_ID, ENTITY_ID)).thenReturn(8L);
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 4L, 8L)).thenReturn(List.of(chunkWithSeq("a", 8L)));

        dispatcher.streamLogTail(TENANT_ID, ENTITY_ID, 0L, u -> { });

        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 9L, 9L)).thenReturn(List.of(chunkWithSeq("b", 9L)));
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 9L, TbCallback.EMPTY);

        ArgumentCaptor<LogsSubscriptionUpdate> captor = ArgumentCaptor.forClass(LogsSubscriptionUpdate.class);
        verify(localSubscriptionService).onLogsUpdate(eq(ENTITY_ID), captor.capture(), any(TbCallback.class));
        assertThat(captor.getValue().getLatestSeq()).isEqualTo(9L);
        assertThat(captor.getValue().getLines()).containsExactly("b0");
    }

    @Test
    void decodesTenantEntityAndSeqFromTheWatermarkProto() {
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 7L)).thenReturn(List.of(chunkWithLines("p", 1)));

        dispatcher.onWatermark(watermarkProto(7L), TbCallback.EMPTY);

        ArgumentCaptor<LogsSubscriptionUpdate> captor = ArgumentCaptor.forClass(LogsSubscriptionUpdate.class);
        verify(localSubscriptionService).onLogsUpdate(eq(ENTITY_ID), captor.capture(), any(TbCallback.class));
        assertThat(captor.getValue().getLatestSeq()).isEqualTo(7L);
        assertThat(captor.getValue().getLines()).containsExactly("p0");
    }

    @Test
    void dropsWatermarkAndAcksWhenTheDeliveryQueueIsFull() {
        DefaultCounter droppedWatermarks = new DefaultCounter(new AtomicInteger(), new SimpleMeterRegistry().counter("dropped"));
        ExecutorService rejecting = MoreExecutors.newDirectExecutorService();
        rejecting.shutdown();
        ReflectionTestUtils.setField(dispatcher, "deliveryExecutor", rejecting);
        ReflectionTestUtils.setField(dispatcher, "droppedWatermarks", droppedWatermarks);

        TbCallback callback = mock(TbCallback.class);
        dispatcher.onWatermark(watermarkProto(7L), callback);

        assertThat(droppedWatermarks.get()).isEqualTo(1);
        verify(callback).onSuccess();
        verify(callback, never()).onFailure(any());
        verifyNoInteractions(localSubscriptionService, logChunkBuffer);
    }

    @Test
    void rewindsTheWatermarkWhenDeliveryFailsSoALaterOneReDeliversTheRange() {
        when(logChunkBuffer.range(TENANT_ID, ENTITY_ID, 1L, 5L))
                .thenThrow(new RuntimeException("buffer unavailable"))
                .thenReturn(List.of(chunkWithLines("x", 1)));

        TbCallback failed = mock(TbCallback.class);
        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 5L, failed);

        verify(failed).onFailure(any(Exception.class));
        verifyNoInteractions(localSubscriptionService);

        dispatcher.onWatermark(TENANT_ID, ENTITY_ID, 5L, TbCallback.EMPTY);

        ArgumentCaptor<LogsSubscriptionUpdate> captor = ArgumentCaptor.forClass(LogsSubscriptionUpdate.class);
        verify(localSubscriptionService).onLogsUpdate(eq(ENTITY_ID), captor.capture(), any(TbCallback.class));
        assertThat(captor.getValue().getLatestSeq()).isEqualTo(5L);
        assertThat(captor.getValue().getLines()).containsExactly("x0");
    }

    private static TbLogStreamUpdateProto watermarkProto(long latestSeq) {
        return TbLogStreamUpdateProto.newBuilder()
                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                .setEntityType(ENTITY_ID.getEntityType().name())
                .setEntityIdMSB(ENTITY_ID.getId().getMostSignificantBits())
                .setEntityIdLSB(ENTITY_ID.getId().getLeastSignificantBits())
                .setLatestSeq(latestSeq)
                .build();
    }

    private static LogChunk chunkWithSeq(String prefix, long seq) {
        LogChunk chunk = chunkWithLines(prefix, 1);
        chunk.setSeq(seq);
        return chunk;
    }

    private static LogChunk chunkWithLines(String prefix, int count) {
        LogChunk chunk = new LogChunk();
        chunk.setLines(IntStream.range(0, count).mapToObj(i -> prefix + i).toList());
        return chunk;
    }
}
