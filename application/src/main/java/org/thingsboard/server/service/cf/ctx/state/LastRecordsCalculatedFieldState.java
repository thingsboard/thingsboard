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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Data
public class LastRecordsCalculatedFieldState extends BaseCalculatedFieldState {

    public LastRecordsCalculatedFieldState() {
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.LAST_RECORDS;
    }

    @Override
    public void initState(Map<String, ArgumentEntry> argumentValues) {
        if (arguments == null) {
            arguments = new TreeMap<>();
        }
        argumentValues.forEach((key, argumentEntry) -> {
            LastRecordsArgumentEntry existingArgumentEntry = (LastRecordsArgumentEntry)
                    arguments.computeIfAbsent(key, k -> new LastRecordsArgumentEntry(new TreeMap<>()));
            if (argumentEntry instanceof LastRecordsArgumentEntry lastRecordsArgumentEntry) {
                existingArgumentEntry.getTsRecords().putAll(lastRecordsArgumentEntry.getTsRecords());
            } else if (argumentEntry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
                existingArgumentEntry.getTsRecords().put(singleValueArgumentEntry.getTs(), singleValueArgumentEntry.getValue());
            }
        });
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(CalculatedFieldCtx ctx) {
        Map<String, Object> resultMap = new HashMap<>();
        arguments.forEach((key, argumentEntry) -> {
            Argument argument = ctx.getArguments().get(key);
            TreeMap<Long, Object> tsRecords = ((LastRecordsArgumentEntry) argumentEntry).getTsRecords();
            if (tsRecords.size() > argument.getLimit()) {
                tsRecords.pollFirstEntry();
            }
            long necessaryIntervalTs = calculateIntervalStart(System.currentTimeMillis(), argument.getTimeWindow());
            tsRecords.entrySet().removeIf(tsRecord -> calculateIntervalStart(tsRecord.getKey(), argument.getTimeWindow()) < necessaryIntervalTs);
            resultMap.put(key, tsRecords);
        });
        Output output = ctx.getOutput();
        return Futures.immediateFuture(new CalculatedFieldResult(output.getType(), output.getScope(), resultMap));
    }

    private long calculateIntervalStart(long ts, long interval) {
        return (ts / interval) * interval;
    }

}
