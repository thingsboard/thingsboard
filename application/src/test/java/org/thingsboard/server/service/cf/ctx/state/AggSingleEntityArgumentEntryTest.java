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
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.AggSingleEntityArgumentEntry;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AggSingleEntityArgumentEntryTest {

    private AggSingleEntityArgumentEntry entry;

    private final DeviceId device1 = new DeviceId(UUID.fromString("1984e5f4-9ff0-4187-84ae-e4438bba4c8a"));

    private final long ts = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        entry = new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 100, new LongDataEntry("key", 12L), 22L));
    }

    @Test
    void testUpdateEntryWhenNotAggEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(new TsRollingArgumentEntry(5, 30000L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for aggregation single entity argument entry: " + ArgumentEntryType.TS_ROLLING);
    }

    @Test
    void testUpdateEntryWhenResetPrevious() {
        AggSingleEntityArgumentEntry singleEntityArgumentEntry = new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 50, new LongDataEntry("key", 18L), 100L));
        singleEntityArgumentEntry.setForceResetPrevious(true);

        assertThat(entry.updateEntry(singleEntityArgumentEntry)).isTrue();
        assertThat(entry.getTs()).isEqualTo(singleEntityArgumentEntry.getTs());
        assertThat(entry.getKvEntryValue()).isEqualTo(singleEntityArgumentEntry.getKvEntryValue());
        assertThat(entry.getVersion()).isEqualTo(singleEntityArgumentEntry.getVersion());
    }


    @Test
    void testUpdateEntryWithTheSameTsAndVersion() {
        assertThat(entry.updateEntry(new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 100, new LongDataEntry("key", 19L), 22L)))).isFalse();
    }

    @Test
    void testUpdateEntryWithTheSameTsAndDifferentVersion() {
        assertThat(entry.updateEntry(new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 100, new LongDataEntry("key", 134L), 23L)))).isTrue();
    }

    @Test
    void testUpdateEntryWhenNewVersionIsNull() {
        assertThat(entry.updateEntry(new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 40, new LongDataEntry("key", 56L), null)))).isTrue();
        assertThat(entry.getValue()).isEqualTo(56L);
        assertThat(entry.getVersion()).isNull();
    }

    @Test
    void testUpdateEntryWhenNewVersionIsGreaterThanCurrent() {
        assertThat(entry.updateEntry(new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 40, new LongDataEntry("key", 76L), 23L)))).isTrue();
        assertThat(entry.getValue()).isEqualTo(76L);
        assertThat(entry.getVersion()).isEqualTo(23);
    }

    @Test
    void testUpdateEntryWhenNewVersionIsLessThanCurrent() {
        assertThat(entry.updateEntry(new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 40, new LongDataEntry("key", 11L), 20L)))).isFalse();
    }

    @Test
    void testUpdateEntryWhenValueWasNotChanged() {
        assertThat(entry.updateEntry(new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 40, new LongDataEntry("key", 18L), 45L)))).isTrue();
    }

    @Test
    void testUpdateEntryWithOldTs() {
        assertThat(entry.updateEntry(new AggSingleEntityArgumentEntry(device1, new BasicTsKvEntry(ts - 150, new LongDataEntry("key", 155L), 45L)))).isFalse();
    }

}
