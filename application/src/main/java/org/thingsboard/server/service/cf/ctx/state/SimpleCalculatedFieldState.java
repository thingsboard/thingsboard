/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbUtils;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class SimpleCalculatedFieldState extends BaseCalculatedFieldState {

    public SimpleCalculatedFieldState(List<String> requiredArguments) {
        super(requiredArguments);
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SIMPLE;
    }

    @Override
    protected void validateNewEntry(ArgumentEntry newEntry) {
        if (newEntry instanceof TsRollingArgumentEntry) {
            throw new IllegalArgumentException("Rolling argument entry is not supported for simple calculated fields.");
        }
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(CalculatedFieldCtx ctx) {
        var expr = ctx.getCustomExpression().get();

        for (Map.Entry<String, ArgumentEntry> entry : this.arguments.entrySet()) {
            try {
                BasicKvEntry kvEntry = ((SingleValueArgumentEntry) entry.getValue()).getKvEntryValue();
                double value = switch (kvEntry.getDataType()) {
                    case LONG -> kvEntry.getLongValue().map(Long::doubleValue).orElseThrow();
                    case DOUBLE -> kvEntry.getDoubleValue().orElseThrow();
                    case BOOLEAN -> kvEntry.getBooleanValue().map(b -> b ? 1.0 : 0.0).orElseThrow();
                    case STRING, JSON -> Double.parseDouble(kvEntry.getValueAsString());
                };
                expr.setVariable(entry.getKey(), value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Argument '" + entry.getKey() + "' is not a number.");
            }
        }

        double expressionResult = expr.evaluate();

        Output output = ctx.getOutput();
        Object result = formatResult(expressionResult, output.getDecimalsByDefault());
        JsonNode outputResult = createResultJson(ctx.isUseLatestTs(), output.getName(), result);

        return Futures.immediateFuture(new CalculatedFieldResult(output.getType(), output.getScope(), outputResult));
    }

    private Object formatResult(double expressionResult, Integer decimals) {
        if (decimals == null) {
            return expressionResult;
        }
        if (decimals.equals(0)) {
            return TbUtils.toInt(expressionResult);
        }
        return TbUtils.toFixed(expressionResult, decimals);
    }

    private JsonNode createResultJson(boolean useLatestTs, String outputName, Object result) {
        ObjectNode valuesNode = JacksonUtil.newObjectNode();
        if (result instanceof Double doubleValue) {
            valuesNode.put(outputName, doubleValue);
        } else if (result instanceof Integer integerValue) {
            valuesNode.put(outputName, integerValue);
        } else {
            valuesNode.set(outputName, JacksonUtil.valueToTree(result));
        }

        long latestTs = getLatestTimestamp();
        if (useLatestTs && latestTs != DEFAULT_LAST_UPDATE_TS) {
            ObjectNode resultNode = JacksonUtil.newObjectNode();
            resultNode.put("ts", latestTs);
            resultNode.set("values", valuesNode);
            return resultNode;
        } else {
            return valuesNode;
        }
    }

}
