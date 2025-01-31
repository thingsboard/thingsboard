/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SingleValueArgumentEntryTest {

    private SingleValueArgumentEntry entry;

    private final long ts = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        entry = new SingleValueArgumentEntry(ts, 11, 363L);
    }

    @Test
    void testArgumentEntryType() {
        assertThat(entry.getType()).isEqualTo(ArgumentEntryType.SINGLE_VALUE);
    }

    @Test
    void testUpdateEntryWhenRollingEntryPassed() {
        assertThatThrownBy(() -> entry.updateEntry(TsRollingArgumentEntry.EMPTY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument entry type for single value argument entry: " + ArgumentEntryType.TS_ROLLING);
    }

    @Test
    void testUpdateEntryWithThaSameTs() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts, 13, 363L))).isFalse();
    }

    @Test
    void testUpdateEntryWhenNewVersionIsNull() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts + 16, 13, null))).isTrue();
        assertThat(entry.getValue()).isEqualTo(13);
        assertThat(entry.getVersion()).isNull();
    }

    @Test
    void testUpdateEntryWhenNewVersionIsGreaterThanCurrent() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts + 18, 18, 369L))).isTrue();
        assertThat(entry.getValue()).isEqualTo(18);
        assertThat(entry.getVersion()).isEqualTo(369L);
    }

    @Test
    void testUpdateEntryWhenNewVersionIsLessThanCurrent() {
        assertThat(entry.updateEntry(new SingleValueArgumentEntry(ts + 18, 18, 234L))).isFalse();
    }

}