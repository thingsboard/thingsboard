// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.processor;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.EdqsObjectKey;
import org.thingsboard.server.edqs.util.VersionsStore;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Covers the delete-tombstone policy that {@link EdqsProcessor#process} delegates to
 * {@link VersionsStore#isNew(EdqsObjectKey, Long, EdqsEventType, boolean)} — the single home for the logic that
 * both the events path and the state/backup path ({@code KafkaEdqsStateService}) share. The producer stamps the
 * {@code versionsResetOnDelete} flag on each event, so both gate positions are pinned here:
 * <ul>
 *   <li>with the flag set on a {@code DELETED} event (row versions may restart, e.g. Citus per-row versions) the
 *       stored version is dropped so a later same-key re-create at v1 is accepted;</li>
 *   <li>with it unset (plain PostgreSQL global-sequence versions) the tombstone version is retained for the TTL
 *       window so a stale update racing the delete stays rejected, while a genuinely newer update is accepted.</li>
 * </ul>
 */
class EdqsProcessorTombstoneTest extends AbstractEdqsProcessorTombstoneTest {

    private final EdqsObjectKey key = new AttributeKv.Key(UUID.randomUUID(), AttributeScope.SERVER_SCOPE, 1);

    @Override
    protected EdqsObjectKey objectKey() {
        return key;
    }

    @Test
    void whenVersionsResetOnDelete_reCreateAtLowerVersionAfterDeleteIsAccepted() {
        processor.process(msg(EdqsEventType.UPDATED, 5L), false);
        // Delete stamped with versionsResetOnDelete=true (Citus per-row versions): the tombstone is cleared.
        processor.process(deleteMsg(6L, true), false);
        // Per-row version reset to 1 on re-create; the delete cleared the tombstone, so this must be accepted.
        processor.process(msg(EdqsEventType.UPDATED, 1L), false);

        verify(repository, times(3)).processEvent(any());
    }

    @Test
    void whenVersionsDoNotResetOnDelete_tombstoneVersionIsRetained() {
        processor.process(msg(EdqsEventType.UPDATED, 5L), false);
        // Delete stamped with versionsResetOnDelete=false (plain PostgreSQL): the tombstone version 6 is kept.
        processor.process(deleteMsg(6L, false), false);
        // Tombstone version 6 is kept for the TTL window: a stale update racing the delete (lower version) stays
        // rejected instead of resurrecting the deleted object. Verified incrementally so the count pins exactly
        // which event was rejected, not just how many passed overall.
        processor.process(msg(EdqsEventType.UPDATED, 4L), false);
        verify(repository, times(2)).processEvent(any());
        // A genuinely newer update (higher than the tombstone version) is still accepted.
        processor.process(msg(EdqsEventType.UPDATED, 7L), false);
        verify(repository, times(3)).processEvent(any());
    }

    @Test
    void staleUpdateWithoutDeleteIsRejected() {
        processor.process(msg(EdqsEventType.UPDATED, 5L), false);
        // No delete in between: the stale lower version is still suppressed by the isNew gate.
        processor.process(msg(EdqsEventType.UPDATED, 1L), false);

        verify(repository, times(1)).processEvent(any());
    }

    private static ToEdqsMsg msg(EdqsEventType eventType, long version) {
        return event(ObjectType.ATTRIBUTE_KV, eventType, version, false);
    }

    private static ToEdqsMsg deleteMsg(long version, boolean versionsResetOnDelete) {
        return event(ObjectType.ATTRIBUTE_KV, EdqsEventType.DELETED, version, versionsResetOnDelete);
    }

}
