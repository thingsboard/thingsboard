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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.SemaphoreWithTbMsgQueue;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "calculate delta",
        version = 1,
        relationTypes = {TbNodeConnectionType.SUCCESS, TbNodeConnectionType.FAILURE, TbNodeConnectionType.OTHER},
        configClazz = CalculateDeltaNodeConfiguration.class,
        nodeDescription = "Calculates delta and amount of time passed between previous timeseries key reading " +
                "and current value for this key from the incoming message",
        nodeDetails = "Useful for metering use cases, when you need to calculate consumption based on pulse counter reading.<br><br>" +
                "Output connections: <code>Success</code>, <code>Other</code> or <code>Failure</code>.",
        configDirective = "tbEnrichmentNodeCalculateDeltaConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/enrichment/calculate-delta/"
)
public class CalculateDeltaNode implements TbNode {

    private Map<EntityId, ValueWithTs> cache;
    private Map<EntityId, SemaphoreWithTbMsgQueue> locks;

    private CalculateDeltaNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, CalculateDeltaNodeConfiguration.class);
        if (StringUtils.isBlank(config.getInputValueKey())) {
            throw new TbNodeException("Input value key should be specified!", true);
        }
        if (StringUtils.isBlank(config.getOutputValueKey())) {
            throw new TbNodeException("Output value key should be specified!", true);
        }
        if (config.isAddPeriodBetweenMsgs() && StringUtils.isBlank(config.getPeriodValueKey())) {
            throw new TbNodeException("Period value key should be specified!", true);
        }
        locks = new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);
        if (config.isUseCache()) {
            cache = new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.SOFT);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.isTypeOf(TbMsgType.POST_TELEMETRY_REQUEST)) {
            ctx.tellNext(msg, TbNodeConnectionType.OTHER);
            return;
        }
        JsonNode msgData = JacksonUtil.toJsonNode(msg.getData());
        if (msgData == null || !msgData.has(config.getInputValueKey())) {
            ctx.tellNext(msg, TbNodeConnectionType.OTHER);
            return;
        }
        locks.computeIfAbsent(msg.getOriginator(), SemaphoreWithTbMsgQueue::new)
                .addToQueueAndTryProcess(msg, ctx, this::processMsgAsync);
    }

    @Override
    public void destroy() {
        locks.clear();
        if (config.isUseCache()) {
            cache.clear();
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                String excludeZeroDeltas = "excludeZeroDeltas";
                if (!oldConfiguration.has(excludeZeroDeltas)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put(excludeZeroDeltas, false);
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

    private ListenableFuture<ValueWithTs> fetchLatestValueAsync(TbContext ctx, EntityId entityId) {
        return Futures.transform(ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, config.getInputValueKey()),
                tsKvEntryOpt -> tsKvEntryOpt.map(this::extractValue).orElse(null), MoreExecutors.directExecutor());
    }

    private ValueWithTs extractValue(TsKvEntry kvEntry) {
        if (kvEntry == null || kvEntry.getValue() == null) {
            return null;
        }
        double result = 0.0;
        long ts = kvEntry.getTs();
        switch (kvEntry.getDataType()) {
            case LONG -> result = kvEntry.getLongValue().get();
            case DOUBLE -> result = kvEntry.getDoubleValue().get();
            case STRING -> {
                try {
                    result = Double.parseDouble(kvEntry.getStrValue().get());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Calculation failed. Unable to parse value [" + kvEntry.getStrValue().get() + "]" +
                            " of telemetry [" + kvEntry.getKey() + "] to Double");
                }
            }
            case BOOLEAN -> throw new IllegalArgumentException("Calculation failed. Boolean values are not supported!");
            case JSON -> throw new IllegalArgumentException("Calculation failed. JSON values are not supported!");
        }
        return new ValueWithTs(ts, result);
    }

    protected ListenableFuture<TbMsg> processMsgAsync(TbContext ctx, TbMsg msg) {
        ListenableFuture<ValueWithTs> latestValueFuture = getLatestFromCacheOrFetchFromDb(ctx, msg);
        return Futures.transform(latestValueFuture, previousData -> {
            ObjectNode json = (ObjectNode) JacksonUtil.toJsonNode(msg.getData());
            double currentValue = json.get(config.getInputValueKey()).asDouble();
            if (config.isUseCache()) {
                cache.put(msg.getOriginator(), new ValueWithTs(msg.getMetaDataTs(), currentValue));
            }
            BigDecimal delta = BigDecimal.valueOf(previousData != null ? currentValue - previousData.value : 0.0);
            if (config.isTellFailureIfDeltaIsNegative() && delta.doubleValue() < 0) {
                throw new IllegalArgumentException("Delta value is negative!");
            }
            if (config.isExcludeZeroDeltas() && delta.doubleValue() == 0) {
                return msg;
            }
            if (config.getRound() != null) {
                delta = delta.setScale(config.getRound(), RoundingMode.HALF_UP);
            }
            if (delta.stripTrailingZeros().scale() > 0) {
                json.put(config.getOutputValueKey(), delta.doubleValue());
            } else {
                json.put(config.getOutputValueKey(), delta.longValueExact());
            }
            if (config.isAddPeriodBetweenMsgs()) {
                long period = previousData != null ? msg.getMetaDataTs() - previousData.ts : 0;
                json.put(config.getPeriodValueKey(), period);
            }
            return msg.transform()
                    .data(JacksonUtil.toString(json))
                    .build();
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<ValueWithTs> getLatestFromCacheOrFetchFromDb(TbContext ctx, TbMsg msg) {
        EntityId originator = msg.getOriginator();
        if (config.isUseCache()) {
            ValueWithTs valueWithTs = cache.get(msg.getOriginator());
            return valueWithTs != null ? Futures.immediateFuture(valueWithTs) : fetchLatestValueAsync(ctx, originator);
        }
        return fetchLatestValueAsync(ctx, originator);
    }

    private record ValueWithTs(long ts, double value) {}

}
