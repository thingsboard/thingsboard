// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfTsDoubleVal;
import org.thingsboard.script.api.tbel.TbelCfTsRollingArg;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState.DEFAULT_LAST_UPDATE_TS;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TsRollingArgumentEntry implements ArgumentEntry {

    private Integer limit;
    private Long timeWindow;
    private TreeMap<Long, Double> tsRecords = new TreeMap<>();

    private boolean forceResetPrevious;

    public TsRollingArgumentEntry(List<TsKvEntry> kvEntries, int limit, long timeWindow) {
        this.limit = limit;
        this.timeWindow = timeWindow;
        kvEntries.forEach(tsKvEntry -> addTsRecord(tsKvEntry.getTs(), tsKvEntry));
    }

    public TsRollingArgumentEntry(TreeMap<Long, Double> tsRecords, int limit, long timeWindow) {
        this.tsRecords = tsRecords;
        this.limit = limit;
        this.timeWindow = timeWindow;
    }

    public TsRollingArgumentEntry(int limit, long timeWindow) {
        this.tsRecords = new TreeMap<>();
        this.limit = limit;
        this.timeWindow = timeWindow;
    }

    public TsRollingArgumentEntry(Integer limit, Long timeWindow, TreeMap<Long, Double> tsRecords) {
        this.limit = limit;
        this.timeWindow = timeWindow;
        this.tsRecords = tsRecords;
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.TS_ROLLING;
    }

    @Override
    public boolean isEmpty() {
        return tsRecords.isEmpty();
    }

    @JsonIgnore
    @Override
    public Object getValue() {
        return tsRecords;
    }

    public long getLatestTs() {
        var lastEntry = tsRecords.lastEntry();
        return (lastEntry != null) ? lastEntry.getKey() : DEFAULT_LAST_UPDATE_TS;
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        List<TbelCfTsDoubleVal> values = new ArrayList<>(tsRecords.size());
        for (var e : tsRecords.entrySet()) {
            values.add(new TbelCfTsDoubleVal(e.getKey(), e.getValue()));
        }
        return new TbelCfTsRollingArg(timeWindow, values);
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
        Double recordValue = getValueForTsRecord(value);
        if (recordValue != null) {
            tsRecords.put(ts, recordValue);
        }
        cleanupExpiredRecords();
    }

    private void addTsRecord(Long ts, double value) {
        tsRecords.put(ts, value);
        cleanupExpiredRecords();
    }

    private void cleanupExpiredRecords() {
        if (tsRecords.size() > limit) {
            tsRecords.pollFirstEntry();
        }
        tsRecords.entrySet().removeIf(tsRecord -> tsRecord.getKey() < System.currentTimeMillis() - timeWindow);
    }

    public static Double getValueForTsRecord(KvEntry value) {
        try {
            return switch (value.getDataType()) {
                case LONG -> value.getLongValue().map(Long::doubleValue).orElse(null);
                case DOUBLE -> value.getDoubleValue().orElse(null);
                case BOOLEAN -> value.getBooleanValue().map(b -> b ? 1.0 : 0.0).orElse(null);
                case STRING -> value.getStrValue().map(Double::parseDouble).orElse(null);
                case JSON -> value.getJsonValue().map(Double::parseDouble).orElse(null);
            };
        } catch (Exception e) {
            log.debug("Invalid value '{}' for time series rolling arguments. Only numeric values are supported.", value.getValue());
            return Double.NaN;
        }
    }

}
