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

import aj.org.objectweb.asm.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class LastRecordsCalculatedFieldState extends BaseCalculatedFieldState {

    private Map<String, List<TsKvEntry>> arguments;

    public LastRecordsCalculatedFieldState() {
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.LAST_RECORDS;
    }


    @Override
    public void initState(Map<String, ArgumentEntry> argumentValues) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        argumentValues.forEach((key, argumentEntry) -> {
            List<TsKvEntry> tsKvEntryList = arguments.computeIfAbsent(key, k -> new ArrayList<>());
//            tsKvEntryList.addAll(argumentEntry.getKvEntries());
        });
    }


    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(CalculationContext ctx) {
        Map<String, Object> resultMap = new HashMap<>();
        arguments.replaceAll((key, entries) -> {
            int limit = ctx.getArguments().get(key).getLimit();
            List<TsKvEntry> limitedEntries = entries.stream()
                    .sorted(Comparator.comparingLong(TsKvEntry::getTs).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            Map<Long, Object> valueWithTs = limitedEntries.stream()
                    .collect(Collectors.toMap(TsKvEntry::getTs, TsKvEntry::getValue));
            resultMap.put(key, valueWithTs);

            return limitedEntries;
        });
        return Futures.immediateFuture(buildResult(ctx.getOutput(), resultMap));
    }

}
