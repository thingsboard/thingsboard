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
package org.thingsboard.server.service.cf.ctx.state.propagation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.PropagationCalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.Map;

import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;

public class PropagationCalculatedFieldState extends ScriptCalculatedFieldState {

    public PropagationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        this.ctx = ctx;
        this.actorCtx = actorCtx;
        this.requiredArguments = ctx.getArgNames();
        if (ctx.isApplyExpressionForResolvedArguments()) {
            this.tbelExpression = ctx.getTbelExpressions().get(ctx.getExpression());
        }
    }

    @Override
    public boolean isReady() {
        if (!super.isReady()) {
            return false;
        }
        ArgumentEntry propagationArg = arguments.get(PROPAGATION_CONFIG_ARGUMENT);
        return propagationArg != null && !propagationArg.isEmpty();
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.PROPAGATION;
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) {
        ArgumentEntry argumentEntry = arguments.get(PROPAGATION_CONFIG_ARGUMENT);
        if (!(argumentEntry instanceof PropagationArgumentEntry propagationArgumentEntry) || propagationArgumentEntry.isEmpty()) {
            return Futures.immediateFuture(PropagationCalculatedFieldResult.builder().build());
        }
        if (ctx.isApplyExpressionForResolvedArguments()) {
            return Futures.transform(super.performCalculation(updatedArgs, ctx), telemetryCfResult ->
                            PropagationCalculatedFieldResult.builder()
                                    .propagationEntityIds(propagationArgumentEntry.getPropagationEntityIds())
                                    .result((TelemetryCalculatedFieldResult) telemetryCfResult)
                                    .build(),
                    MoreExecutors.directExecutor());
        }
        return Futures.immediateFuture(PropagationCalculatedFieldResult.builder()
                .propagationEntityIds(propagationArgumentEntry.getPropagationEntityIds())
                .result(toTelemetryResult(ctx))
                .build());
    }

    private TelemetryCalculatedFieldResult toTelemetryResult(CalculatedFieldCtx ctx) {
        Output output = ctx.getOutput();
        TelemetryCalculatedFieldResult.TelemetryCalculatedFieldResultBuilder telemetryCfBuilder =
                TelemetryCalculatedFieldResult.builder()
                        .type(output.getType())
                        .scope(output.getScope());
        ObjectNode valuesNode = JacksonUtil.newObjectNode();
        arguments.forEach((outputKey, argumentEntry) -> {
            if (argumentEntry instanceof PropagationArgumentEntry) {
                return;
            }
            if (argumentEntry instanceof SingleValueArgumentEntry singleArgumentEntry) {
                JacksonUtil.addKvEntry(valuesNode, singleArgumentEntry.getKvEntryValue(), outputKey);
                return;
            }
            throw new IllegalArgumentException("Unsupported argument type: " + argumentEntry.getType() + " detected for argument: " + outputKey + ". " +
                                               "Only Latest telemetry or Attribute arguments supported for 'Arguments Only' propagation mode!");
        });
        ObjectNode result = toSimpleResult(output.getType() == OutputType.TIME_SERIES, valuesNode);
        telemetryCfBuilder.result(result);
        return telemetryCfBuilder.build();
    }

}
