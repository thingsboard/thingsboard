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
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfPropagationArg;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationArgumentEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PropagationArgumentEntryTest {

    private final AssetId ENTITY_1_ID = new AssetId(UUID.fromString("b0a8637d-6d67-43d5-a483-c0e391afe805"));
    private final AssetId ENTITY_2_ID = new AssetId(UUID.fromString("7bd85073-ded5-414f-a2ef-bd56ad3dbf6a"));
    private final AssetId ENTITY_3_ID = new AssetId(UUID.fromString("d64f3e51-2ec2-472f-b475-b095ef8bdc70"));

    private PropagationArgumentEntry entry;

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
        assertThat(entry.getValue()).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<AssetId> value = (List<AssetId>) entry.getValue();
        assertThat(value).containsExactly(ENTITY_1_ID, ENTITY_2_ID);
    }

    @Test
    void testUpdateEntryWhenSingleEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(new SingleValueArgumentEntry()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for propagation argument entry: SINGLE_VALUE");
    }

    @Test
    void testUpdateEntryWhenRollingEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(new TsRollingArgumentEntry(5, 30000L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for propagation argument entry: TS_ROLLING");
    }

    @Test
    void testUpdateEntryReplacesWithNewIds() {
        var newIds = new ArrayList<EntityId>(List.of(ENTITY_3_ID, ENTITY_1_ID));
        var updated = new PropagationArgumentEntry(newIds);

        boolean changed = entry.updateEntry(updated);

        assertThat(changed).isTrue();
        assertThat(entry.getPropagationEntityIds()).containsExactlyElementsOf(newIds);
    }

    @Test
    void testUpdateEntryClearsWhenNewEntryIsEmpty() {
        var updatedEmpty = new PropagationArgumentEntry(List.of());

        boolean changed = entry.updateEntry(updatedEmpty);

        assertThat(changed).isTrue();
        assertThat(entry.getPropagationEntityIds()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testToTbelCfArgWithValues() {
        TbelCfArg arg = entry.toTbelCfArg();
        assertThat(arg).isInstanceOf(TbelCfPropagationArg.class);

        TbelCfPropagationArg tbelCfPropagationArg = (TbelCfPropagationArg) arg;
        assertThat(tbelCfPropagationArg.getValue()).isInstanceOf(List.class);
        assertThat((List<EntityId>) tbelCfPropagationArg.getValue()).containsExactly(ENTITY_1_ID, ENTITY_2_ID);
    }


    @Test
    @SuppressWarnings("unchecked")
    void testToTbelCfArgWithEmptyValues() {
        var empty = new PropagationArgumentEntry(List.of());
        TbelCfArg emptyArg = empty.toTbelCfArg();
        assertThat(emptyArg).isInstanceOf(TbelCfPropagationArg.class);

        TbelCfPropagationArg tbelCfPropagationArg = (TbelCfPropagationArg) emptyArg;
        assertThat(tbelCfPropagationArg.getValue()).isInstanceOf(List.class);
        assertThat((List<EntityId>) tbelCfPropagationArg.getValue()).isEmpty();
    }

    @Test
    void testAddNewPropagationEntityIdToEmptyArgument() {
        PropagationArgumentEntry empty = new PropagationArgumentEntry(List.of());
        assertThat(empty.addPropagationEntityId(ENTITY_1_ID)).isTrue();
        assertThat(empty.getPropagationEntityIds()).containsExactly(ENTITY_1_ID);
    }

    @Test
    void testAddNewPropagationEntityIdThatAlreadyExists() {
        PropagationArgumentEntry hasEntity = new PropagationArgumentEntry(List.of(ENTITY_1_ID));
        assertThat(hasEntity.addPropagationEntityId(ENTITY_1_ID)).isFalse();
        assertThat(hasEntity.getPropagationEntityIds()).containsExactly(ENTITY_1_ID);
    }

    @Test
    void testAddNewPropagationEntityId() {
        PropagationArgumentEntry hasEntity = new PropagationArgumentEntry(List.of(ENTITY_1_ID, ENTITY_2_ID));
        assertThat(hasEntity.addPropagationEntityId(ENTITY_3_ID)).isTrue();
        assertThat(hasEntity.getPropagationEntityIds()).contains(ENTITY_1_ID, ENTITY_2_ID, ENTITY_3_ID);
    }

    @Test
    void testRemovePropagationEntityId() {
        PropagationArgumentEntry hasEntity = new PropagationArgumentEntry(List.of(ENTITY_1_ID));
        hasEntity.removePropagationEntityId(ENTITY_1_ID);
        assertThat(hasEntity.isEmpty()).isTrue();
    }

}
