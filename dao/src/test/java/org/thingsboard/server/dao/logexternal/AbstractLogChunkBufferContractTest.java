// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.logexternal;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behaviour both {@link LogChunkBuffer} implementations must share. The caffeine and redis buffers are a parallel
 * pair: {@code LogStreamDispatcher} derives its evicted-chunk count from the difference between the requested range
 * and what {@code range} returns, so any divergence between them changes what users see depending on deployment mode.
 */
public abstract class AbstractLogChunkBufferContractTest {

    protected static final int MAX_CHUNKS_PER_UNIT = 3;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    protected abstract LogChunkBuffer buffer();

    private EntityId newUnitId() {
        return new AgentAppUnitId(UUID.randomUUID());
    }

    private LogChunk chunk(String line, long lastLineTs) {
        return new LogChunk("unit", List.of(line), 0, lastLineTs);
    }

    @Test
    void appendAssignsSeqsStartingAtOneAndStampsTheChunk() {
        EntityId unitId = newUnitId();
        LogChunk first = chunk("a", 10);
        LogChunk second = chunk("b", 20);

        assertThat(buffer().append(TENANT_ID, unitId, first)).isEqualTo(1L);
        assertThat(buffer().append(TENANT_ID, unitId, second)).isEqualTo(2L);

        List<LogChunk> retained = buffer().range(TENANT_ID, unitId, 1, 2);
        assertThat(retained).extracting(LogChunk::getSeq).containsExactly(1L, 2L);
        assertThat(retained).extracting(c -> c.getLines().get(0)).containsExactly("a", "b");
    }

    @Test
    void rangeBoundsAreInclusive() {
        EntityId unitId = newUnitId();
        appendLines(unitId, "a", "b", "c");

        assertThat(lines(buffer().range(TENANT_ID, unitId, 1, 3))).containsExactly("a", "b", "c");
        assertThat(lines(buffer().range(TENANT_ID, unitId, 2, 2))).containsExactly("b");
        assertThat(lines(buffer().range(TENANT_ID, unitId, 2, 3))).containsExactly("b", "c");
    }

    @Test
    void rangeIsEmptyWhenToIsBelowFrom() {
        EntityId unitId = newUnitId();
        appendLines(unitId, "a");

        assertThat(buffer().range(TENANT_ID, unitId, 5, 4)).isEmpty();
    }

    @Test
    void rangeOfAnUnknownUnitIsEmpty() {
        assertThat(buffer().range(TENANT_ID, newUnitId(), 1, 10)).isEmpty();
    }

    @Test
    void oldestChunksAreTrimmedPastTheCap() {
        EntityId unitId = newUnitId();
        appendLines(unitId, "a", "b", "c", "d", "e");

        assertThat(lines(buffer().range(TENANT_ID, unitId, 1, 5))).containsExactly("c", "d", "e");
        assertThat(buffer().latestTailSeq(TENANT_ID, unitId)).isEqualTo(5L);
    }

    @Test
    void rangeOverAPartiallyEvictedWindowReturnsOnlyWhatIsRetained() {
        EntityId unitId = newUnitId();
        appendLines(unitId, "a", "b", "c", "d", "e");

        List<LogChunk> retained = buffer().range(TENANT_ID, unitId, 1, 4);

        assertThat(lines(retained)).containsExactly("c", "d");
        assertThat(retained).extracting(LogChunk::getSeq).containsExactly(3L, 4L);
    }

    @Test
    void latestTailSeqAndLineTsAreZeroForAnEmptyTail() {
        EntityId unitId = newUnitId();

        assertThat(buffer().latestTailSeq(TENANT_ID, unitId)).isZero();
        assertThat(buffer().latestTailLineTs(TENANT_ID, unitId)).isZero();
    }

    @Test
    void latestTailLineTsComesFromTheNewestRetainedChunk() {
        EntityId unitId = newUnitId();
        buffer().append(TENANT_ID, unitId, chunk("a", 111));
        buffer().append(TENANT_ID, unitId, chunk("b", 222));

        assertThat(buffer().latestTailLineTs(TENANT_ID, unitId)).isEqualTo(222L);
    }

    @Test
    void deleteUnitDropsChunksAndResetsTheSeqCounter() {
        EntityId unitId = newUnitId();
        appendLines(unitId, "a", "b");

        buffer().deleteUnit(TENANT_ID, unitId);

        assertThat(buffer().range(TENANT_ID, unitId, 1, 10)).isEmpty();
        assertThat(buffer().latestTailSeq(TENANT_ID, unitId)).isZero();
        assertThat(buffer().append(TENANT_ID, unitId, chunk("c", 30))).isEqualTo(1L);
    }

    @Test
    void unitsAreIsolatedFromEachOther() {
        EntityId first = newUnitId();
        EntityId second = newUnitId();
        appendLines(first, "a", "b");
        appendLines(second, "x");

        assertThat(lines(buffer().range(TENANT_ID, first, 1, 10))).containsExactly("a", "b");
        assertThat(lines(buffer().range(TENANT_ID, second, 1, 10))).containsExactly("x");
        assertThat(buffer().latestTailSeq(TENANT_ID, second)).isEqualTo(1L);
    }

    private void appendLines(EntityId unitId, String... lines) {
        long ts = 1;
        for (String line : lines) {
            buffer().append(TENANT_ID, unitId, chunk(line, ts++));
        }
    }

    private static List<String> lines(List<LogChunk> chunks) {
        return chunks.stream().map(c -> c.getLines().get(0)).toList();
    }
}
