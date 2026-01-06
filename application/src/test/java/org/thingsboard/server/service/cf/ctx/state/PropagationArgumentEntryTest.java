/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cf.ctx.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfPropagationArg;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationArgumentEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class PropagationArgumentEntryTest {

    private final AssetId ENTITY_1_ID = new AssetId(UUID.fromString("b0a8637d-6d67-43d5-a483-c0e391afe805"));
    private final AssetId ENTITY_2_ID = new AssetId(UUID.fromString("7bd85073-ded5-414f-a2ef-bd56ad3dbf6a"));
    private final AssetId ENTITY_3_ID = new AssetId(UUID.fromString("d64f3e51-2ec2-472f-b475-b095ef8bdc70"));

    private PropagationArgumentEntry entry;

    @Mock
    private CalculatedFieldCtx ctx;

    @BeforeEach
    void setUp() {
        List<EntityId> propagationEntityIds = new ArrayList<>();
        propagationEntityIds.add(ENTITY_1_ID);
        propagationEntityIds.add(ENTITY_2_ID);
        entry = new PropagationArgumentEntry(propagationEntityIds);
    }

    @Test
    void testArgumentEntryType() {
        assertThat(entry.getType()).isEqualTo(ArgumentEntryType.PROPAGATION);
    }

    @Test
    void testIsEmpty() {
        PropagationArgumentEntry emptyEntry = new PropagationArgumentEntry(List.of());
        assertThat(emptyEntry.isEmpty()).isTrue();
    }

    @Test
    void testGetValueReturnsPropagationIds() {
        assertThat(entry.getValue()).isInstanceOf(Set.class);
        @SuppressWarnings("unchecked")
        Set<AssetId> value = (Set<AssetId>) entry.getValue();
        assertThat(value).containsExactlyInAnyOrder(ENTITY_1_ID, ENTITY_2_ID);
    }

    @Test
    void testUpdateEntryWhenSingleEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(new SingleValueArgumentEntry(), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for propagation argument entry: SINGLE_VALUE");
    }

    @Test
    void testUpdateEntryWhenRollingEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(new TsRollingArgumentEntry(5, 30000L), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for propagation argument entry: TS_ROLLING");
    }

    @Test
    void testUpdateEntryReplacesWithNewIds() {
        var newIds = new ArrayList<EntityId>(List.of(ENTITY_3_ID, ENTITY_1_ID));
        var updated = new PropagationArgumentEntry(newIds);

        boolean changed = entry.updateEntry(updated, ctx);

        assertThat(changed).isTrue();
        assertThat(entry.getEntityIds()).containsExactlyElementsOf(newIds);
    }

    @Test
    void testUpdateEntryClearsWhenNewEntryIsEmpty() {
        var updatedEmpty = new PropagationArgumentEntry(List.of());

        boolean changed = entry.updateEntry(updatedEmpty, ctx);

        assertThat(changed).isTrue();
        assertThat(entry.getEntityIds()).isEmpty();
    }

    @Test
    void testUpdateEntryWhenAdded() {
        var added = new PropagationArgumentEntry();
        added.setAdded(List.of(ENTITY_3_ID));

        boolean changed = entry.updateEntry(added, ctx);

        assertThat(changed).isTrue();
        assertThat(entry.getEntityIds()).containsExactlyInAnyOrder(ENTITY_1_ID, ENTITY_2_ID, ENTITY_3_ID);
        assertThat(entry.getAdded()).isEqualTo(List.of(ENTITY_3_ID));
    }

    @Test
    void testUpdateEntryWhenAddedExistingEntity() {
        var added = new PropagationArgumentEntry();
        added.setAdded(List.of(ENTITY_2_ID));

        boolean changed = entry.updateEntry(added, ctx);

        assertThat(changed).isFalse();
        assertThat(entry.getEntityIds()).containsExactlyInAnyOrder(ENTITY_1_ID, ENTITY_2_ID);
        assertThat(entry.getAdded()).isNull();
    }

    @Test
    void testUpdateEntryWhenRemoved() {
        var removed = new PropagationArgumentEntry();
        removed.setRemoved(ENTITY_2_ID);

        boolean changed = entry.updateEntry(removed, ctx);

        assertThat(changed).isTrue();
        assertThat(entry.getEntityIds()).containsExactlyInAnyOrder(ENTITY_1_ID);
        assertThat(entry.getRemoved()).isNull();
    }

    @Test
    void testUpdateEntryWhenRemovedNonExistingEntity() {
        var removed = new PropagationArgumentEntry();
        removed.setRemoved(ENTITY_3_ID);

        boolean changed = entry.updateEntry(removed, ctx);

        assertThat(changed).isFalse();
        assertThat(entry.getEntityIds()).containsExactlyInAnyOrder(ENTITY_1_ID, ENTITY_2_ID);
        assertThat(entry.getRemoved()).isNull();
    }

    @Test
    void testUpdateEntryWhenPartitionStateRestoreAddsMissingIds() {
        var restore = new PropagationArgumentEntry(List.of(ENTITY_1_ID, ENTITY_2_ID, ENTITY_3_ID));
        restore.setSyncWithDb(true);

        boolean changed = entry.updateEntry(restore, ctx);

        assertThat(changed).isTrue();
        assertThat(entry.getEntityIds()).containsExactlyInAnyOrder(ENTITY_1_ID, ENTITY_2_ID, ENTITY_3_ID);
        assertThat(entry.getAdded()).containsExactly(ENTITY_3_ID);
        assertThat(entry.getRemoved()).isNull();
        assertThat(entry.isSyncWithDb()).isFalse();
    }

    @Test
    void testUpdateEntryWhenPartitionStateRestoreRemovesStaleIds() {
        var restore = new PropagationArgumentEntry(List.of(ENTITY_1_ID));
        restore.setSyncWithDb(true);

        boolean changed = entry.updateEntry(restore, ctx);

        assertThat(changed).isTrue(); // expected to be changed, so we re-check readiness for the state
        assertThat(entry.getEntityIds()).containsExactlyInAnyOrder(ENTITY_1_ID);
        assertThat(entry.getAdded()).isNull();
        assertThat(entry.getRemoved()).isNull();
        assertThat(entry.isSyncWithDb()).isFalse();
    }

    @Test
    void testUpdateEntryWhenPartitionStateRestoreAddsAndRemoves() {
        var restore = new PropagationArgumentEntry(List.of(ENTITY_1_ID, ENTITY_3_ID));
        restore.setSyncWithDb(true);

        boolean changed = entry.updateEntry(restore, ctx);

        assertThat(changed).isTrue();
        assertThat(entry.getEntityIds()).containsExactlyInAnyOrder(ENTITY_1_ID, ENTITY_3_ID);
        assertThat(entry.getAdded()).containsExactly(ENTITY_3_ID);
        assertThat(entry.getRemoved()).isNull();
        assertThat(entry.isSyncWithDb()).isFalse();
    }


    @Test
    void testUpdateEntryWhenPartitionStateRestoreNoChanges() {
        var restore = new PropagationArgumentEntry(List.of(ENTITY_1_ID, ENTITY_2_ID));
        restore.setSyncWithDb(true);

        boolean changed = entry.updateEntry(restore, ctx);

        assertThat(changed).isFalse();
        assertThat(entry.getEntityIds()).containsExactlyInAnyOrder(ENTITY_1_ID, ENTITY_2_ID);
        assertThat(entry.getAdded()).isNull();
        assertThat(entry.getRemoved()).isNull();
        assertThat(entry.isSyncWithDb()).isFalse();
    }

    @Test
    void testUpdateEntryWhenPartitionStateRestoreEmptySet() {
        var restore = new PropagationArgumentEntry(List.of());
        restore.setSyncWithDb(true);

        boolean changed = entry.updateEntry(restore, ctx);

        assertThat(changed).isTrue(); // expected to be changed, so we re-check readiness for the state
        assertThat(entry.getEntityIds()).isEmpty();
        assertThat(entry.getAdded()).isNull();
        assertThat(entry.getRemoved()).isNull();
        assertThat(entry.isSyncWithDb()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testToTbelCfArgWithValues() {
        TbelCfArg arg = entry.toTbelCfArg();
        assertThat(arg).isInstanceOf(TbelCfPropagationArg.class);

        TbelCfPropagationArg tbelCfPropagationArg = (TbelCfPropagationArg) arg;
        assertThat(tbelCfPropagationArg.getValue()).isInstanceOf(Set.class);
        assertThat((Set<EntityId>) tbelCfPropagationArg.getValue()).containsExactlyInAnyOrder(ENTITY_1_ID, ENTITY_2_ID);
    }


    @Test
    @SuppressWarnings("unchecked")
    void testToTbelCfArgWithEmptyValues() {
        var empty = new PropagationArgumentEntry(List.of());
        TbelCfArg emptyArg = empty.toTbelCfArg();
        assertThat(emptyArg).isInstanceOf(TbelCfPropagationArg.class);

        TbelCfPropagationArg tbelCfPropagationArg = (TbelCfPropagationArg) emptyArg;
        assertThat(tbelCfPropagationArg.getValue()).isInstanceOf(Set.class);
        assertThat((Set<EntityId>) tbelCfPropagationArg.getValue()).isEmpty();
    }

}
