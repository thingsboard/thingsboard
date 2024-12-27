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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TsRollingArgumentEntry implements ArgumentEntry {

    public static final ArgumentEntry EMPTY = new TsRollingArgumentEntry(0);

    private static final int MAX_ROLLING_ARGUMENT_ENTRY_SIZE = 1000;

    private TreeMap<Long, Object> tsRecords = new TreeMap<>();

    public TsRollingArgumentEntry(List<TsKvEntry> kvEntries) {
        kvEntries.forEach(tsKvEntry -> addTsRecord(tsKvEntry.getTs(), tsKvEntry.getValue()));
    }

    /**
     * Internal constructor to create immutable TsRollingArgumentEntry.EMPTY
     */
    private TsRollingArgumentEntry(int ignored) {
        this.tsRecords = new TreeMap<>();
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.TS_ROLLING;
    }

    @JsonIgnore
    @Override
    public Object getValue() {
        return tsRecords;
    }

    @Override
    public boolean hasUpdatedValue(ArgumentEntry entry) {
        return entry instanceof SingleValueArgumentEntry ?
                !tsRecords.containsKey(((SingleValueArgumentEntry) entry).getTs()) :
                !tsRecords.keySet().containsAll(((TsRollingArgumentEntry) entry).getTsRecords().keySet());
    }

    @Override
    public ArgumentEntry copy() {
        return new TsRollingArgumentEntry(new TreeMap<>(tsRecords));
    }

    public void addTsRecord(Long key, Object value) {
        if (NumberUtils.isParsable(value.toString())) {
            tsRecords.put(key, value);
            if (tsRecords.size() > MAX_ROLLING_ARGUMENT_ENTRY_SIZE) {
                tsRecords.pollFirstEntry();
            }
        } else {
            log.warn("Argument type 'TS_ROLLING' only supports numeric values.");
        }
    }

    public void addAllTsRecords(Map<Long, Object> newRecords) {
        for (Map.Entry<Long, Object> entry : newRecords.entrySet()) {
            addTsRecord(entry.getKey(), entry.getValue());
        }
    }

}
