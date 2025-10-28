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
package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Setter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldProcessingService;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;

public class EntityAggregationCalculatedFieldState extends BaseCalculatedFieldState {

    private Map<AggIntervalEntry, Boolean> aggIntervals = new HashMap<>();

    @Setter
    private long lastArgsRefreshTs = -1;
    @Setter
    private long lastMetricsEvalTs = -1;

    private long deduplicationIntervalMs = -1;

    CalculatedFieldProcessingService cfProcessingService;

    public EntityAggregationCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        super.setCtx(ctx, actorCtx);
        this.cfProcessingService = ctx.getCfProcessingService();
        var configuration = (EntityAggregationCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        deduplicationIntervalMs = SECONDS.toMillis(configuration.getDeduplicationIntervalInSec());
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ENTITY_AGGREGATION;
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) throws Exception {
        long endTs = System.currentTimeMillis();
        long startTs = endTs - 1000;
        AggIntervalEntry interval = new AggIntervalEntry();

        Map<String, ArgumentEntry> metrics = cfProcessingService.fetchArgumentValuesDuringInterval(entityId, interval, ctx);

        Output output = ctx.getOutput();
        lastMetricsEvalTs = System.currentTimeMillis();
        ctx.scheduleReevaluation(deduplicationIntervalMs, actorCtx);
        ObjectNode result = toResult(endTs, metrics);
        if (result != null) {
            return Futures.immediateFuture(TelemetryCalculatedFieldResult.builder()
                    .type(output.getType())
                    .scope(output.getScope())
                    .result(result)
                    .build());
        }
        return Futures.immediateFuture(TelemetryCalculatedFieldResult.EMPTY);
    }


    protected ObjectNode toResult(long endTs, Map<String, ArgumentEntry> metrics) {
        ObjectNode metricsNode = JacksonUtil.newObjectNode();
        for (Map.Entry<String, ArgumentEntry> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            ArgumentEntry argumentEntry = entry.getValue();
            if (!argumentEntry.isEmpty()) {
                metricsNode.put(metricName, JacksonUtil.toString(argumentEntry.getValue()));
            }
        }
        ObjectNode resultNode = JacksonUtil.newObjectNode();
        if (!metricsNode.isEmpty()) {
            resultNode.put("ts", endTs);
            resultNode.set("values", metricsNode);
        }
        return resultNode;
    }


    @Override
    public boolean isReady() {
        return true;
    }

}
