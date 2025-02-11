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
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mvel2.execution.ExecutionArrayList;
import org.thingsboard.script.api.tbel.TbCfArg;
import org.thingsboard.script.api.tbel.TbCfSingleValueArg;
import org.thingsboard.script.api.tbel.TbCfTsRollingArg;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@Slf4j
@NoArgsConstructor
public class ScriptCalculatedFieldState extends BaseCalculatedFieldState {

    public ScriptCalculatedFieldState(List<String> requiredArguments) {
        super(requiredArguments);
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SCRIPT;
    }

    @Override
    protected void validateNewEntry(ArgumentEntry newEntry) {
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(CalculatedFieldCtx ctx) {
        arguments.forEach((key, argumentEntry) -> {
            if (argumentEntry instanceof TsRollingArgumentEntry tsRollingEntry) {
                Argument argument = ctx.getArguments().get(key);
                TreeMap<Long, BasicKvEntry> tsRecords = tsRollingEntry.getTsRecords();
                if (tsRecords.size() > argument.getLimit()) {
                    tsRecords.pollFirstEntry();
                }
                tsRecords.entrySet().removeIf(tsRecord -> tsRecord.getKey() < System.currentTimeMillis() - argument.getTimeWindow());
            }
        });
        Object[] args = ctx.getArgNames().stream()
                .map(this::toTbelArgument)
                .toArray();

        ListenableFuture<Map<String, Object>> resultFuture = ctx.getCalculatedFieldScriptEngine().executeToMapAsync(args);
        Output output = ctx.getOutput();
        return Futures.transform(resultFuture,
                result -> new CalculatedFieldResult(output.getType(), output.getScope(), result),
                MoreExecutors.directExecutor()
        );
    }

    private TbCfArg toTbelArgument(String key) {
        ArgumentEntry argEntry = arguments.get(key);
        if (argEntry instanceof SingleValueArgumentEntry svArg) {
            return new TbCfSingleValueArg(svArg.getTs(), argEntry.getValue());
        } else if (argEntry instanceof TsRollingArgumentEntry rollingArg) {
            var tsRecords = rollingArg.getTsRecords();
            List<TbCfSingleValueArg> values = new ArrayList<>(tsRecords.size());
            for(var e : tsRecords.entrySet()){
                values.add(new TbCfSingleValueArg(e.getKey(), e.getValue().getValue()));
            }
            return new TbCfTsRollingArg(values);
        } else {
            throw new RuntimeException("Argument is not supported for TBEL execution!");
        }
    }

}
