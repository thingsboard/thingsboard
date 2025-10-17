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
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.AggArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.AggSingleEntityArgumentEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AggArgumentEntryTest {

    private AggArgumentEntry entry;

    private final DeviceId device1 = new DeviceId(UUID.fromString("1984e5f4-9ff0-4187-84ae-e4438bba4c8a"));
    private final DeviceId device2 = new DeviceId(UUID.fromString("937fc062-1a9d-438f-aa22-55a93fc908b7"));

    private final long ts = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        Map<EntityId, ArgumentEntry> aggInputs = new HashMap<>();
        aggInputs.put(device1, new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 100, new LongDataEntry("key", 12L), 1L)));
        aggInputs.put(device2, new AggSingleEntityArgumentEntry(device2, new BasicTsKvEntry(ts - 150, new LongDataEntry("key", 16L), 6L)));

        entry = new AggArgumentEntry(aggInputs, false);
    }

    @Test
    void testUpdateEntryWhenNotAggEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(new TsRollingArgumentEntry(5, 30000L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for aggregation argument entry: " + ArgumentEntryType.TS_ROLLING);
    }

    @Test
    void testUpdateEntryWhenAggArgumentEntryPasser() {
        DeviceId device3 = new DeviceId(UUID.randomUUID());
        DeviceId device4 = new DeviceId(UUID.randomUUID());

        AggArgumentEntry aggArgumentEntry = new AggArgumentEntry(Map.of(
                device3, new AggSingleEntityArgumentEntry(device3, new BasicTsKvEntry(ts - 50, new LongDataEntry("key", 16L), 13L)),
                device4, new AggSingleEntityArgumentEntry(device4, new BasicTsKvEntry(ts - 60, new LongDataEntry("key", 23L), 7L))
        ), false);

        assertThat(entry.updateEntry(aggArgumentEntry)).isTrue();

        Map<EntityId, ArgumentEntry> aggInputs = entry.getAggInputs();
        assertThat(aggInputs.size()).isEqualTo(4);
        assertThat(aggInputs.get(device3)).isEqualTo(aggArgumentEntry.getAggInputs().get(device3));
        assertThat(aggInputs.get(device4)).isEqualTo(aggArgumentEntry.getAggInputs().get(device4));
    }

    @Test
    void testUpdateEntryWhenAggSingleEntityArgumentEntryPassedAndNoEntriesById() {
        DeviceId device3 = new DeviceId(UUID.randomUUID());

        AggSingleEntityArgumentEntry singleEntityArgumentEntry = new AggSingleEntityArgumentEntry(device3, new BasicTsKvEntry(ts - 50, new LongDataEntry("key", 18L), 10L));

        assertThat(entry.updateEntry(singleEntityArgumentEntry)).isTrue();

        Map<EntityId, ArgumentEntry> aggInputs = entry.getAggInputs();
        assertThat(aggInputs.size()).isEqualTo(3);
        assertThat(aggInputs.get(device3)).isEqualTo(singleEntityArgumentEntry);
    }

    @Test
    void testUpdateEntryWhenAggSingleEntityArgumentEntryPassedAndEntryByIdExist() {
        AggSingleEntityArgumentEntry singleEntityArgumentEntry = new AggSingleEntityArgumentEntry(device2, new BasicTsKvEntry(ts - 50, new LongDataEntry("key", 18L), 10L));

        assertThat(entry.updateEntry(singleEntityArgumentEntry)).isTrue();

        Map<EntityId, ArgumentEntry> aggInputs = entry.getAggInputs();
        assertThat(aggInputs.size()).isEqualTo(2);
        assertThat(aggInputs.get(device2)).isEqualTo(singleEntityArgumentEntry);
    }

    @Test
    void testUpdateEntryWhenDeletedAggSingleEntityArgumentEntryPassed() {
        AggSingleEntityArgumentEntry singleEntityArgumentEntry = new AggSingleEntityArgumentEntry(device2, true);

        assertThat(entry.updateEntry(singleEntityArgumentEntry)).isTrue();

        Map<EntityId, ArgumentEntry> aggInputs = entry.getAggInputs();
        assertThat(aggInputs.size()).isEqualTo(1);
        assertThat(aggInputs.get(device2)).isNull();
    }

}
