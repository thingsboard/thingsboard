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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
public class ScriptCalculatedFieldState implements CalculatedFieldState {

    @JsonIgnore
    private CalculatedFieldScriptEngine calculatedFieldScriptEngine;

    private Map<String, KvEntry> arguments;

    public ScriptCalculatedFieldState() {
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SCRIPT;
    }


    @Override
    public void initState(Map<String, ArgumentEntry> argumentValues) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        argumentValues.forEach((key, value) -> arguments.put(key, value.getKvEntry()));
    }


    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(CalculationContext ctx) {
        CalculatedFieldConfiguration calculatedFieldConfiguration = ctx.getConfiguration();
        TbelInvokeService tbelInvokeService = ctx.getTbelInvokeService();

        if (tbelInvokeService == null) {
            throw new IllegalArgumentException("TBEL script engine is disabled!");
        }

        if (calculatedFieldScriptEngine == null) {
            initEngine(ctx.getTenantId(), calculatedFieldConfiguration, tbelInvokeService);
        }

        ListenableFuture<Object> resultFuture = calculatedFieldScriptEngine.executeScriptAsync(arguments);

        return Futures.transform(resultFuture, result -> {
            Output output = calculatedFieldConfiguration.getOutput();
            Map<String, Object> resultMap = result instanceof Map<?, ?>
                    ? JacksonUtil.convertValue(result, Map.class)
                    : new HashMap<>();

            CalculatedFieldResult calculatedFieldResult = new CalculatedFieldResult();
            calculatedFieldResult.setType(output.getType());
            calculatedFieldResult.setScope(output.getScope());
            calculatedFieldResult.setResultMap(resultMap);

            return calculatedFieldResult;
        }, MoreExecutors.directExecutor());
    }

    private void initEngine(TenantId tenantId, CalculatedFieldConfiguration calculatedFieldConfiguration, TbelInvokeService tbelInvokeService) {
        calculatedFieldScriptEngine = new CalculatedFieldTbelScriptEngine(
                tenantId,
                tbelInvokeService,
                calculatedFieldConfiguration.getExpression(),
                arguments.keySet().toArray(new String[0])
        );
    }

}
