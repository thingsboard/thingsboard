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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.SemaphoreWithTbMsgQueue;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "calculate delta",
        relationTypes = {TbNodeConnectionType.SUCCESS, TbNodeConnectionType.FAILURE, TbNodeConnectionType.OTHER},
        configClazz = CalculateDeltaNodeConfiguration.class,
        nodeDescription = "Calculates delta and amount of time passed between previous timeseries key reading " +
                "and current value for this key from the incoming message",
        nodeDetails = "Useful for metering use cases, when you need to calculate consumption based on pulse counter reading.<br><br>" +
                "Output connections: <code>Success</code>, <code>Other</code> or <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeCalculateDeltaConfig")
public class CalculateDeltaNode implements TbNode {

    private Map<EntityId, ValueWithTs> cache;
    private Map<EntityId, SemaphoreWithTbMsgQueue> locks;

    private CalculateDeltaNodeConfiguration config;
    private TbContext ctx;
    private TimeseriesService timeseriesService;
    private boolean useCache;
    private String inputKey;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, CalculateDeltaNodeConfiguration.class);
        this.ctx = ctx;
        this.timeseriesService = ctx.getTimeseriesService();
        this.inputKey = config.getInputValueKey();
        this.useCache = config.isUseCache();
        if (useCache) {
            locks = new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);
            cache = new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.SOFT);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.isTypeOf(TbMsgType.POST_TELEMETRY_REQUEST)) {
            ctx.tellNext(msg, TbNodeConnectionType.OTHER);
            return;
        }
        JsonNode json = JacksonUtil.toJsonNode(msg.getData());
        if (!json.has(inputKey)) {
            ctx.tellNext(msg, TbNodeConnectionType.OTHER);
            return;
        }
        if (useCache) {
            var semaphoreWithQueue = locks.computeIfAbsent(msg.getOriginator(), SemaphoreWithTbMsgQueue::new);
            semaphoreWithQueue.addToQueueAndTryProcess(msg, ctx, this::processMsgAsync);
            return;
        }
        withCallback(fetchLatestValueAsync(msg.getOriginator()),
                previousData -> {
                    processCalculateDelta(msg.getOriginator(), msg.getMetaDataTs(), (ObjectNode) json, previousData);
                    ctx.tellSuccess(TbMsg.transformMsgData(msg, JacksonUtil.toString(json)));
                },
                t -> ctx.tellFailure(msg, t), MoreExecutors.directExecutor());
    }

    @Override
    public void destroy() {
        if (useCache) {
            cache.clear();
            locks.clear();
        }
    }

    private ListenableFuture<ValueWithTs> fetchLatestValueAsync(EntityId entityId) {
        return Futures.transform(timeseriesService.findLatest(ctx.getTenantId(), entityId, config.getInputValueKey()),
                tsKvEntryOpt -> tsKvEntryOpt.map(this::extractValue).orElse(null), ctx.getDbCallbackExecutor());
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

    private void processCalculateDelta(EntityId originator, long msgTs, ObjectNode json, ValueWithTs previousData) {
        double currentValue = json.get(inputKey).asDouble();
        if (useCache) {
            cache.put(originator, new ValueWithTs(msgTs, currentValue));
        }
        BigDecimal delta = BigDecimal.valueOf(previousData != null ? currentValue - previousData.value : 0.0);
        if (config.isTellFailureIfDeltaIsNegative() && delta.doubleValue() < 0) {
            throw new IllegalArgumentException("Delta value is negative!");
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
            long period = previousData != null ? msgTs - previousData.ts : 0;
            json.put(config.getPeriodValueKey(), period);
        }
    }

    protected ListenableFuture<TbMsg> processMsgAsync(TbContext ctx, TbMsg msg) {
        ListenableFuture<ValueWithTs> latestValueFuture = getLatestFromCacheOrFetchFromDb(msg);
        return Futures.transform(latestValueFuture, previousData -> {
            ObjectNode json = (ObjectNode) JacksonUtil.toJsonNode(msg.getData());
            processCalculateDelta(msg.getOriginator(), msg.getMetaDataTs(), json, previousData);
            return TbMsg.transformMsgData(msg, JacksonUtil.toString(json));
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<ValueWithTs> getLatestFromCacheOrFetchFromDb(TbMsg msg) {
        EntityId originator = msg.getOriginator();
        ValueWithTs valueWithTs = cache.get(msg.getOriginator());
        return valueWithTs != null ? Futures.immediateFuture(valueWithTs) : fetchLatestValueAsync(originator);
    }

    private record ValueWithTs(long ts, double value) {
    }

}
