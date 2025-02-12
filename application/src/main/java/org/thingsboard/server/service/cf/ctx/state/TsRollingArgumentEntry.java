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
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfTsDoubleVal;
import org.thingsboard.script.api.tbel.TbelCfTsRollingArg;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.util.ProtoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TsRollingArgumentEntry implements ArgumentEntry {

    public static final ArgumentEntry EMPTY = new TsRollingArgumentEntry(0);

    private static final int MAX_ROLLING_ARGUMENT_ENTRY_SIZE = 1000;

    private TreeMap<Long, Double> tsRecords = new TreeMap<>();

    public TsRollingArgumentEntry(List<TsKvEntry> kvEntries) {
        kvEntries.forEach(tsKvEntry -> addTsRecord(tsKvEntry.getTs(), tsKvEntry));
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
    public TbelCfArg toTbelCfArg() {
        List<TbelCfTsDoubleVal> values = new ArrayList<>(tsRecords.size());
        for (var e : tsRecords.entrySet()) {
            values.add(new TbelCfTsDoubleVal(e.getKey(), e.getValue()));
        }
        return new TbelCfTsRollingArg(values);
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (entry instanceof TsRollingArgumentEntry tsRollingEntry) {
            updateTsRollingEntry(tsRollingEntry);
        } else if (entry instanceof SingleValueArgumentEntry singleValueEntry) {
            updateSingleValueEntry(singleValueEntry);
        } else {
            throw new IllegalArgumentException("Unsupported argument entry type for rolling argument entry: " + entry.getType());
        }
        return true;
    }

    private void updateTsRollingEntry(TsRollingArgumentEntry tsRollingEntry) {
        for (Map.Entry<Long, Double> tsRecordEntry : tsRollingEntry.getTsRecords().entrySet()) {
            addTsRecord(tsRecordEntry.getKey(), tsRecordEntry.getValue());
        }
    }

    private void updateSingleValueEntry(SingleValueArgumentEntry singleValueEntry) {
        addTsRecord(singleValueEntry.getTs(), singleValueEntry.getKvEntryValue());
    }

    private void addTsRecord(Long ts, KvEntry value) {
        switch (value.getDataType()) {
            case LONG -> value.getLongValue().ifPresent(aLong -> tsRecords.put(ts, aLong.doubleValue()));
            case DOUBLE -> value.getDoubleValue().ifPresent(aDouble -> tsRecords.put(ts, aDouble));
            case BOOLEAN -> value.getBooleanValue().ifPresent(aBoolean -> tsRecords.put(ts, aBoolean ? 1.0 : 0.0));
            case STRING -> value.getStrValue().ifPresent(aString -> tsRecords.put(ts, Double.parseDouble(aString)));
            case JSON -> value.getJsonValue().ifPresent(aString -> tsRecords.put(ts, Double.parseDouble(aString)));
            //TODO: try catch
        }
        pollFirstEntryIfNeeded();
    }

    private void addTsRecord(Long ts, double value) {
        tsRecords.put(ts, value);
        pollFirstEntryIfNeeded();
    }

    private void pollFirstEntryIfNeeded() {
        if (tsRecords.size() > MAX_ROLLING_ARGUMENT_ENTRY_SIZE) {
            tsRecords.pollFirstEntry();
        }
    }

}
