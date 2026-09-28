// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEvent;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.EdqsObjectKey;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Relation counterpart of {@link EdqsProcessorTombstoneTest}. Locks in the existing {@link EdqsProcessor#process}
 * dedup behavior for a {@code RELATION} object: on a {@code DELETED} event stamped with
 * {@code versionsResetOnDelete} by the producer the stored version is removed ({@code versionsStore.remove(key)}),
 * so a later same-key re-create — whose per-row version may have reset to a low value under Citus, where relation
 * versions are per-row rather than global — is accepted by the {@code isNew} gate rather than wrongly suppressed
 * as stale. This guards the conclusion that the relation per-row version is safe for EDQS dedup. It asserts
 * current behavior only and must not drive any production change.
 */
class EdqsProcessorRelationTombstoneTest extends AbstractEdqsProcessorTombstoneTest {

    // Tracks the events the processor actually let through to the repository, so the tests can observe whether an
    // event passed the isNew gate (accepted) and whether the relation is currently present (last accepted event
    // was an upsert, not a delete).
    private int acceptedCount;
    private EdqsEventType lastAcceptedEventType;

    // Fixed from/to/typeGroup/type so every event below targets the same EntityRelation key.
    private final EdqsObjectKey key = new EntityRelation.Key(
            UUID.randomUUID(), UUID.randomUUID(), RelationTypeGroup.COMMON, "Contains");

    @Override
    protected EdqsObjectKey objectKey() {
        return key;
    }

    @BeforeEach
    void setUp() {
        // The repository only sees events that survived the isNew/tombstone gate, so recording its calls is the
        // same observation point EdqsProcessorTombstoneTest uses (verify(repository).processEvent(...)).
        doAnswer(invocation -> {
            EdqsEvent event = invocation.getArgument(0);
            acceptedCount++;
            lastAcceptedEventType = event.getEventType();
            return null;
        }).when(repository).processEvent(any());
    }

    @Test
    void deleteThenRecreateWithLowerVersionIsAccepted() {
        // 1. upsert relation r at version 5 -> accepted
        assertThat(process(relationUpsertEvent(5L))).isTrue();
        assertThat(stored()).isTrue();

        // 2. delete relation r at its stored version -> accepted, tombstone cleared (versionsStore.remove(key))
        assertThat(process(relationDeleteEvent(5L))).isTrue();
        assertThat(stored()).isFalse();

        // 3. re-create relation r at version 1 (per-row counter restarts low) -> MUST be accepted,
        //    because the tombstone was removed on delete (not suppressed as "stale").
        assertThat(process(relationUpsertEvent(1L))).isTrue();
        assertThat(stored()).isTrue();
    }

    @Test
    void staleDuplicateForSameRelationIsSuppressed() {
        assertThat(process(relationUpsertEvent(5L))).isTrue();
        // No delete in between: a re-delivery at a strictly lower version for a still-present relation is deduped
        // by the isNew gate (prevVersion.value <= version is false for a lower version). Equal versions are
        // accepted per the VersionsStore contract, so a strictly lower version exercises suppression here.
        assertThat(process(relationUpsertEvent(1L))).isFalse();
    }

    /**
     * Drives the real processor and reports whether the event passed the isNew/tombstone gate, observed via the
     * repository call the processor makes only for accepted events.
     */
    private boolean process(ToEdqsMsg msg) {
        int before = acceptedCount;
        processor.process(msg, false);
        return acceptedCount > before;
    }

    /**
     * Whether the relation is currently present in EDQS: at least one event has been accepted and the most
     * recent accepted event was an upsert rather than a delete.
     */
    private boolean stored() {
        return lastAcceptedEventType != null && lastAcceptedEventType != EdqsEventType.DELETED;
    }

    private static ToEdqsMsg relationUpsertEvent(long version) {
        return event(ObjectType.RELATION, EdqsEventType.UPDATED, version, false);
    }

    private static ToEdqsMsg relationDeleteEvent(long version) {
        // Relation versions are per-row under Citus, so the producer stamps versionsResetOnDelete on the delete.
        return event(ObjectType.RELATION, EdqsEventType.DELETED, version, true);
    }

}
