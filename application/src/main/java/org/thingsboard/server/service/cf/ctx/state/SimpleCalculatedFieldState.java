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
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.HashMap;
import java.util.Map;

@Data
public class SimpleCalculatedFieldState implements CalculatedFieldState {

    private Map<String, KvEntry> arguments;

    public SimpleCalculatedFieldState() {
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SIMPLE;
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

        Output output = calculatedFieldConfiguration.getOutput();
        Map<String, Argument> arguments = calculatedFieldConfiguration.getArguments();

        if (isValid(this.arguments, arguments)) {
            CalculatedFieldResult result = new CalculatedFieldResult();
            String expression = calculatedFieldConfiguration.getExpression();
            ThreadLocal<Expression> customExpression = new ThreadLocal<>();
            var expr = customExpression.get();
            if (expr == null) {
                expr = new ExpressionBuilder(expression)
                        .implicitMultiplication(true)
                        .variables(this.arguments.keySet())
                        .build();
                customExpression.set(expr);
            }
            Map<String, Double> variables = new HashMap<>();
            this.arguments.forEach((k, v) -> variables.put(k, Double.parseDouble(v.getValueAsString())));
            expr.setVariables(variables);

            double expressionResult = expr.evaluate();

            result.setType(output.getType());
            result.setScope(output.getScope());
            result.setResultMap(Map.of(output.getName(), expressionResult));
            return Futures.immediateFuture(result);
        }
        return null;
    }

}
