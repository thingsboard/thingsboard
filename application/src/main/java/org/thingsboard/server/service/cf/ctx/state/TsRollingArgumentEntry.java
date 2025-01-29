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
    public ArgumentEntry copy() {
        return new TsRollingArgumentEntry(new TreeMap<>(tsRecords));
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (entry instanceof TsRollingArgumentEntry tsRollingEntry) {
            return updateTsRollingEntry(tsRollingEntry);
        } else if (entry instanceof SingleValueArgumentEntry singleValueEntry) {
            return updateSingleValueEntry(singleValueEntry);
        } else {
            throw new IllegalArgumentException("Unsupported argument entry type for rolling argument entry: " + entry.getType());
        }
    }

    private boolean updateTsRollingEntry(TsRollingArgumentEntry tsRollingEntry) {
        boolean updated = false;
        for (Map.Entry<Long, Object> tsRecordEntry : tsRollingEntry.getTsRecords().entrySet()) {
            updated |= addTsRecordIfAbsent(tsRecordEntry.getKey(), tsRecordEntry.getValue());
        }
        return updated;
    }

    private boolean updateSingleValueEntry(SingleValueArgumentEntry singleValueEntry) {
        return addTsRecordIfAbsent(singleValueEntry.getTs(), singleValueEntry.getValue());
    }

    private boolean addTsRecordIfAbsent(Long ts, Object value) {
        if (!tsRecords.containsKey(ts)) {
            addTsRecord(ts, value);
            return true;
        }
        return false;
    }

    private void addTsRecord(Long ts, Object value) {
        if (NumberUtils.isParsable(value.toString())) {
            tsRecords.put(ts, value);
            if (tsRecords.size() > MAX_ROLLING_ARGUMENT_ENTRY_SIZE) {
                tsRecords.pollFirstEntry();
            }
        } else {
            throw new IllegalArgumentException("Argument type " + getType() + " only supports numeric values.");
        }
    }

}
