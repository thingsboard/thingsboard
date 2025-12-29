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
import org.thingsboard.script.api.tbel.TbelCfSingleValueArg;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class SingleValueArgumentEntryTest {

    private SingleValueArgumentEntry entry;

    @Mock
    private CalculatedFieldCtx ctx;

    private final long ts = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        entry = new SingleValueArgumentEntry(ts, new LongDataEntry("key", 11L), 363L);
    }

    @Test
    void testArgumentEntryType() {
        assertThat(entry.getType()).isEqualTo(ArgumentEntryType.SINGLE_VALUE);
    }

    @Test
    void testUpdateEntryWhenRollingEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(new TsRollingArgumentEntry(5, 30000L), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for single value argument entry: " + ArgumentEntryType.TS_ROLLING);
    }

    @Test
    void testUpdateEntryWithTheSameTs() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts, new LongDataEntry("key", 13L), 363L), ctx)).isFalse();
    }

    @Test
    void testUpdateEntryWithTheSameTsAndDifferentVersion() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts, new LongDataEntry("key", 13L), 364L), ctx)).isTrue();
    }

    @Test
    void testUpdateEntryWhenNewVersionIsNull() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts + 16, new LongDataEntry("key", 13L), null), ctx)).isTrue();
        assertThat(entry.getValue()).isEqualTo(13L);
        assertThat(entry.getVersion()).isNull();
    }

    @Test
    void testUpdateEntryWhenNewVersionIsGreaterThanCurrent() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts + 18, new LongDataEntry("key", 18L), 369L), ctx)).isTrue();
        assertThat(entry.getValue()).isEqualTo(18L);
        assertThat(entry.getVersion()).isEqualTo(369L);
    }

    @Test
    void testUpdateEntryWhenNewVersionIsLessThanCurrent() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts + 18, new LongDataEntry("key", 18L), 234L), ctx)).isFalse();
    }

    @Test
    void testUpdateEntryWhenValueWasNotChanged() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts + 18, new LongDataEntry("key", 11L), 364L), ctx)).isTrue();
    }

    @Test
    void testUpdateEntryWithOldTs() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts - 10, new LongDataEntry("key", 14L), 365L), ctx)).isFalse();
    }

    @Test
    void testToTbelCfArgWhenJsonIsObject() {
        entry = new SingleValueArgumentEntry(ts, new JsonDataEntry("key", "{\"test\": 10}"), 370L);
        TbelCfArg tbelCfArg = entry.toTbelCfArg();
        assertThat(tbelCfArg).isNotNull();
        assertThat(tbelCfArg).isInstanceOf(TbelCfSingleValueArg.class);

        TbelCfSingleValueArg singleValueArg = (TbelCfSingleValueArg) tbelCfArg;

        assertThat(singleValueArg.getValue()).isInstanceOf(Map.class);
        Map<String, Integer> expectedMap = Map.of("test", 10);
        assertThat(singleValueArg.getValue()).isEqualTo(expectedMap);
    }

    @Test
    void testToTbelCfArgWhenJsonIsArray() {
        entry = new SingleValueArgumentEntry(ts, new JsonDataEntry("key", "[{\"test\": 10}, {\"test2\": 20}]"), 371L);
        TbelCfArg tbelCfArg = entry.toTbelCfArg();
        assertThat(tbelCfArg).isNotNull();
        assertThat(tbelCfArg).isInstanceOf(TbelCfSingleValueArg.class);

        TbelCfSingleValueArg singleValueArg = (TbelCfSingleValueArg) tbelCfArg;

        assertThat(singleValueArg.getValue()).isInstanceOf(List.class);
        List<Map<String, Integer>> expectedList = new ArrayList<>();
        expectedList.add(Map.of("test", 10));
        expectedList.add(Map.of("test2", 20));
        assertThat(singleValueArg.getValue()).isEqualTo(expectedList);
    }

}
