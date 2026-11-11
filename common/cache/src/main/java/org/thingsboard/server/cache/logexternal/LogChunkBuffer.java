// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.logexternal;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;

import java.util.List;

public interface LogChunkBuffer {

    /**
     * Appends a chunk for the given unit and returns the monotonically increasing
     * seq assigned by the buffer. Seq is unit-scoped and survives gRPC session changes.
     */
    long append(TenantId tenantId, EntityId unitId, LogChunk chunk);

    /**
     * Returns retained chunks within the given seq range. Used by the watermark
     * broadcast path to fetch the delta since the last watermark; chunks evicted
     * past the per-unit cap are simply absent from the result.
     */
    List<LogChunk> range(TenantId tenantId, EntityId unitId, long fromSeqInclusive, long toSeqInclusive);

    /**
     * Returns the highest seq currently in the retained tail for this unit,
     * or 0 if the tail is empty.
     */
    long latestTailSeq(TenantId tenantId, EntityId unitId);

    /**
     * Returns the {@code lastLineTs} of the most recent retained chunk for this
     * unit, or 0 if no tail is retained.
     */
    long latestTailLineTs(TenantId tenantId, EntityId unitId);

    /**
     * Drops all retained chunks and the seq counter for the given unit.
     * Called when the unit itself is removed so retained logs do not outlive it.
     */
    void deleteUnit(TenantId tenantId, EntityId unitId);

}
