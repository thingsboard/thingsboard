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
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.List;
import java.util.Map;

@Data
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
        String expression = ctx.getExpression();
        ThreadLocal<Expression> customExpression = ctx.getCustomExpression();
        var expr = customExpression.get();
        if (expr == null) {
            expr = new ExpressionBuilder(expression)
                    .implicitMultiplication(true)
                    .variables(this.arguments.keySet())
                    .build();
            customExpression.set(expr);
        }

        for (Map.Entry<String, ArgumentEntry> entry : this.arguments.entrySet()) {
            try {
                expr.setVariable(entry.getKey(), Double.parseDouble(entry.getValue().getValue().toString()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Argument '" + entry.getKey() + "' is not a number.");
            }
        }

        double expressionResult = expr.evaluate();

        Output output = ctx.getOutput();
        return Futures.immediateFuture(new CalculatedFieldResult(output.getType(), output.getScope(), Map.of(output.getName(), expressionResult)));
    }

}
