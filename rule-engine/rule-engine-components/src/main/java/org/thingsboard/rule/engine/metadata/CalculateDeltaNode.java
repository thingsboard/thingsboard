/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "calculate delta",
        configClazz = CalculateDeltaNodeConfiguration.class,
        nodeDescription = "Calculates and adds 'delta' value into message based on the incoming and previous value",
        nodeDetails = "Calculates delta and period based on the previous time-series reading and current data. " +
                "Delta calculation is done in scope of the message originator, e.g. device, asset or customer.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeCalculateDeltaConfig")
public class CalculateDeltaNode implements TbNode {
    private Map<EntityId, ValueWithTs> cache;
    private CalculateDeltaNodeConfiguration config;
    private TbContext ctx;
    private TimeseriesService timeseriesService;
    private boolean useCache;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, CalculateDeltaNodeConfiguration.class);
        this.ctx = ctx;
        this.timeseriesService = ctx.getTimeseriesService();
        this.useCache = config.isUseCache();

        if (useCache) {
            cache = new ConcurrentHashMap<>();
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        JsonNode json = JacksonUtil.toJsonNode(msg.getData());
        String inputKey = config.getInputValueKey();
        if (json.has(inputKey)) {
            DonAsynchron.withCallback(getLastValue(msg.getOriginator()),
                    previousData -> {
                        double currentValue = json.get(inputKey).asDouble();
                        long currentTs = TbMsgTimeseriesNode.getTs(msg);

                        if (useCache) {
                            cache.put(msg.getOriginator(), new ValueWithTs(currentTs, currentValue));
                        }

                        BigDecimal delta = BigDecimal.valueOf(previousData != null ? currentValue - previousData.value : 0.0);

                        if (config.isTellFailureIfDeltaIsNegative() && delta.doubleValue() < 0) {
                            ctx.tellNext(msg, TbRelationTypes.FAILURE);
                            return;
                        }

                        if (config.getRound() != null) {
                            delta = delta.setScale(config.getRound(), RoundingMode.HALF_UP);
                        }

                        ObjectNode result = (ObjectNode) json;
                        result.put(config.getOutputValueKey(), delta);

                        if (config.isAddPeriodBetweenMsgs()) {
                            long period = previousData != null ? currentTs - previousData.ts : 0;
                            result.put(config.getPeriodValueKey(), period);
                        }
                        ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), JacksonUtil.toString(result)));
                    },
                    t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } else if (config.isTellFailureIfInputValueKeyIsAbsent()) {
            ctx.tellNext(msg, TbRelationTypes.FAILURE);
        } else {
            ctx.tellSuccess(msg);
        }
    }

    @Override
    public void destroy() {
        if (useCache) {
            cache.clear();
        }
    }

    private ListenableFuture<ValueWithTs> fetchLatestValue(EntityId entityId) {
        return Futures.transform(timeseriesService.findLatest(ctx.getTenantId(), entityId, Collections.singletonList(config.getInputValueKey())),
                list -> extractValue(list.get(0))
                , ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<ValueWithTs> getLastValue(EntityId entityId) {
        ValueWithTs latestValue;
        if (useCache && (latestValue = cache.get(entityId)) != null) {
            return Futures.immediateFuture(latestValue);
        } else {
            return fetchLatestValue(entityId);
        }
    }

    private ValueWithTs extractValue(TsKvEntry kvEntry) {
        if (kvEntry == null || kvEntry.getValue() == null) {
            return null;
        }
        double result = 0.0;
        long ts = kvEntry.getTs();
        switch (kvEntry.getDataType()) {
            case LONG:
                result = kvEntry.getLongValue().get();
                break;
            case DOUBLE:
                result = kvEntry.getDoubleValue().get();
                break;
            case STRING:
                try {
                    result = Double.parseDouble(kvEntry.getStrValue().get());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Calculation failed. Unable to parse value [" + kvEntry.getStrValue().get() + "]" +
                            " of telemetry [" + kvEntry.getKey() + "] to Double");
                }
                break;
            case BOOLEAN:
                throw new IllegalArgumentException("Calculation failed. Boolean values are not supported!");
            case JSON:
                throw new IllegalArgumentException("Calculation failed. JSON values are not supported!");
        }
        return new ValueWithTs(ts, result);
    }

    private static class ValueWithTs {
        private final long ts;
        private final double value;

        private ValueWithTs(long ts, double value) {
            this.ts = ts;
            this.value = value;
        }
    }
}
