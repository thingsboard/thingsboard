/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TsRollingArgumentEntryTest {

    private TsRollingArgumentEntry entry;

    @Mock
    private CalculatedFieldCtx ctx;

    private final long ts = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        TreeMap<Long, Double> values = new TreeMap<>();
        values.put(ts - 40, 10.0);
        values.put(ts - 30, 12.0);
        values.put(ts - 20, 17.0);

        entry = new TsRollingArgumentEntry(5, 30000L, values);
    }

    @Test
    void testArgumentEntryType() {
        assertThat(entry.getType()).isEqualTo(ArgumentEntryType.TS_ROLLING);
    }

    @Test
    void testUpdateEntryWhenSingleValueEntryPassed() {
        SingleValueArgumentEntry newEntry = new SingleValueArgumentEntry(ts - 10, new DoubleDataEntry("key", 23.0), 123L);

        assertThat(entry.updateEntry(newEntry, ctx)).isTrue();
        assertThat(entry.getTsRecords()).hasSize(4);
        assertThat(entry.getTsRecords().get(ts - 10)).isEqualTo(23.0);
    }

    @Test
    void testUpdateEntryWhenRollingEntryPassed() {
        TsRollingArgumentEntry newEntry = new TsRollingArgumentEntry();
        TreeMap<Long, Double> values = new TreeMap<>();
        values.put(ts - 10, 7.0);
        values.put(ts - 5, 1.0);
        newEntry.setTsRecords(values);

        assertThat(entry.updateEntry(newEntry, ctx)).isTrue();
        assertThat(entry.getTsRecords()).hasSize(5);
        assertThat(entry.getTsRecords()).isEqualTo(Map.of(
                ts - 40, 10.0,
                ts - 30, 12.0,
                ts - 20, 17.0,
                ts - 10, 7.0,
                ts - 5, 1.0
        ));
    }

    @Test
    void testUpdateEntryWhenValueIsNotNumber() {
        SingleValueArgumentEntry newEntry = new SingleValueArgumentEntry(ts - 10, new StringDataEntry("key", "string"), 123L);

        assertThat(entry.updateEntry(newEntry, ctx)).isTrue();
        assertThat(entry.getTsRecords().get(ts - 10)).isNaN();
    }

    @Test
    void testUpdateEntryWhenOldTelemetry() {
        TsRollingArgumentEntry newEntry = new TsRollingArgumentEntry();
        TreeMap<Long, Double> values = new TreeMap<>();
        values.put(ts - 40000, 4.0);// will not be used for calculation
        values.put(ts - 45000, 2.0);// will not be used for calculation
        values.put(ts - 5, 0.0);
        newEntry.setTsRecords(values);

        entry = new TsRollingArgumentEntry(3, 30000L);
        assertThat(entry.updateEntry(newEntry, ctx)).isTrue();
        assertThat(entry.getTsRecords()).hasSize(1);
        assertThat(entry.getTsRecords()).isEqualTo(Map.of(
                ts - 5, 0.0
        ));
    }

    @Test
    void testPerformCalculationWhenArgumentsMoreThanLimit() {
        TsRollingArgumentEntry newEntry = new TsRollingArgumentEntry();
        TreeMap<Long, Double> values = new TreeMap<>();
        values.put(ts - 20, 1000.0);// will not be used
        values.put(ts - 18, 0.0);
        values.put(ts - 16, 0.0);
        values.put(ts - 14, 0.0);
        newEntry.setTsRecords(values);

        entry = new TsRollingArgumentEntry(3, 30000L);
        assertThat(entry.updateEntry(newEntry, ctx)).isTrue();
        assertThat(entry.getTsRecords()).hasSize(3);
        assertThat(entry.getTsRecords()).isEqualTo(Map.of(
                ts - 18, 0.0,
                ts - 16, 0.0,
                ts - 14, 0.0
        ));
    }

}