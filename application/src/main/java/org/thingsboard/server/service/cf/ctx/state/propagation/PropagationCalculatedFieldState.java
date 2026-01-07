/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.service.cf.CalculatedFieldProcessingService;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.PropagationCalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;

@Slf4j
public class PropagationCalculatedFieldState extends ScriptCalculatedFieldState {

    private CalculatedFieldProcessingService cfProcessingService;

    public PropagationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        this.ctx = ctx;
        this.actorCtx = actorCtx;
        this.cfProcessingService = ctx.getCfProcessingService();
        this.requiredArguments = new ArrayList<>(ctx.getArgNames());
        requiredArguments.add(PROPAGATION_CONFIG_ARGUMENT);
        this.readinessStatus = checkReadiness();
        if (ctx.isApplyExpressionForResolvedArguments()) {
            this.tbelExpression = ctx.getTbelExpressions().get(ctx.getExpression());
        }
    }

    @Override
    public void init(boolean restored) {
        super.init(restored);
        if (restored) {
            cfProcessingService.fetchPropagationArgumentFromDb(ctx, entityId).ifPresent(fromDb -> {
                fromDb.setSyncWithDb(true);
                var updatedArgs = update(Map.of(PROPAGATION_CONFIG_ARGUMENT, fromDb), ctx);
                if (updatedArgs.isEmpty()) {
                    return;
                }
                log.warn("[{}][{}] Propagation argument was out of sync during state restore and was reconciled with DB.", ctx.getCfId(), entityId);
                var updatedPropagationArgument = (PropagationArgumentEntry) arguments.get(PROPAGATION_CONFIG_ARGUMENT);
                if (updatedPropagationArgument.getAdded() != null) {
                    log.warn("[{}][{}] New propagation entities were detected during restore. Scheduling reevaluation for new entities...", ctx.getCfId(), entityId);
                    ctx.scheduleReevaluation(0L, actorCtx);
                }
            });
        }
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.PROPAGATION;
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) {
        ArgumentEntry argumentEntry = arguments.get(PROPAGATION_CONFIG_ARGUMENT);
        if (!(argumentEntry instanceof PropagationArgumentEntry propagationArgumentEntry)) {
            return Futures.immediateFuture(PropagationCalculatedFieldResult.builder().build());
        }
        boolean newEntityAdded = propagationArgumentEntry.getAdded() != null;
        List<EntityId> entityIds;
        if (newEntityAdded) {
            entityIds = propagationArgumentEntry.getAdded();
            propagationArgumentEntry.setAdded(null);
        } else {
            if (propagationArgumentEntry.getEntityIds().isEmpty()) {
                return Futures.immediateFuture(PropagationCalculatedFieldResult.builder().build());
            }
            entityIds = List.copyOf(propagationArgumentEntry.getEntityIds());
        }
        if (ctx.isApplyExpressionForResolvedArguments()) {
            return Futures.transform(super.performCalculation(updatedArgs, ctx), telemetryCfResult ->
                            PropagationCalculatedFieldResult.builder()
                                    .entityIds(entityIds)
                                    .result((TelemetryCalculatedFieldResult) telemetryCfResult)
                                    .build(),
                    MoreExecutors.directExecutor());
        }
        if (newEntityAdded || CollectionsUtil.isEmpty(updatedArgs)) {
            updatedArgs = arguments;
        }
        return Futures.immediateFuture(PropagationCalculatedFieldResult.builder()
                .entityIds(entityIds)
                .result(toTelemetryResult(ctx, updatedArgs))
                .build());
    }

    @Override
    protected ReadinessStatus checkReadiness() {
        if (ctx.isApplyExpressionForResolvedArguments() || arguments == null) {
            return super.checkReadiness();
        }
        boolean propagationNotEmpty = false;
        boolean hasOtherNonEmpty = false;
        List<String> emptyArguments = null;
        for (String requiredArgumentKey : requiredArguments) {
            ArgumentEntry argumentEntry = arguments.get(requiredArgumentKey);
            if (argumentEntry == null || argumentEntry.isEmpty()) {
                if (emptyArguments == null) {
                    emptyArguments = new ArrayList<>();
                }
                emptyArguments.add(requiredArgumentKey);
            } else if (PROPAGATION_CONFIG_ARGUMENT.equals(requiredArgumentKey)) {
                propagationNotEmpty = true;
            } else {
                hasOtherNonEmpty = true;
            }
        }
        if (propagationNotEmpty && hasOtherNonEmpty) {
            return ReadinessStatus.READY;
        }
        return ReadinessStatus.from(emptyArguments);
    }

    private TelemetryCalculatedFieldResult toTelemetryResult(CalculatedFieldCtx ctx, Map<String, ArgumentEntry> updatedArgs) {
        Output output = ctx.getOutput();
        TelemetryCalculatedFieldResult.TelemetryCalculatedFieldResultBuilder telemetryCfBuilder =
                TelemetryCalculatedFieldResult.builder()
                        .outputStrategy(output.getStrategy())
                        .type(output.getType())
                        .scope(output.getScope());
        ObjectNode valuesNode = JacksonUtil.newObjectNode();
        updatedArgs.forEach((outputKey, argumentEntry) -> {
            if (argumentEntry instanceof PropagationArgumentEntry) {
                return;
            }
            if (argumentEntry instanceof SingleValueArgumentEntry singleArgumentEntry) {
                if (!singleArgumentEntry.isEmpty()) {
                    JacksonUtil.addKvEntry(valuesNode, singleArgumentEntry.getKvEntryValue(), outputKey);
                }
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
