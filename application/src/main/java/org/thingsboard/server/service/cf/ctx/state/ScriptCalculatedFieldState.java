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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.Map;
import java.util.TreeMap;

@Data
@Slf4j
public class ScriptCalculatedFieldState extends BaseCalculatedFieldState {

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SCRIPT;
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(CalculatedFieldCtx ctx) {
        arguments.forEach((key, argumentEntry) -> {
            if (argumentEntry instanceof TsRollingArgumentEntry) {
                Argument argument = ctx.getArguments().get(key);
                TreeMap<Long, Object> tsRecords = ((TsRollingArgumentEntry) argumentEntry).getTsRecords();
                if (tsRecords.size() > argument.getLimit()) {
                    tsRecords.pollFirstEntry();
                }
                tsRecords.entrySet().removeIf(tsRecord -> tsRecord.getKey() < System.currentTimeMillis() - argument.getTimeWindow());
            }
        });
        Object[] args = ctx.getArgKeys().stream()
                .map(key -> arguments.get(key).getValue())
                .toArray();
        ListenableFuture<Map<String, Object>> resultFuture = ctx.getCalculatedFieldScriptEngine().executeToMapAsync(args);
        Output output = ctx.getOutput();
        return Futures.transform(resultFuture,
                result -> new CalculatedFieldResult(output.getType(), output.getScope(), result),
                MoreExecutors.directExecutor()
        );
    }

}
