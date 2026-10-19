// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "aggregate latest",
        version = 1,
        hasQueueName = true,
        configClazz = TbAggLatestTelemetryNodeV2Configuration.class,
        nodeDescription = "Aggregates data based on incoming event",
        nodeDetails = "Performs aggregation of attributes or latest time-series fetched from related entities. " +
                "Generates outgoing message with aggregated values. By default, an outgoing message generates with 'POST_TELEMETRY_REQUEST' type. " +
                "The type of the outgoing messages controls under \"<b>Output message type</b>\" configuration parameter.",
        configDirective = "tbAnalyticsNodeAggregateLatestV2Config",
        icon = "functions",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/analytics/aggregate-latest/"
)
public class TbAggLatestTelemetryNodeV2 implements TbNode {

    private final Gson gson = new Gson();
    private static final int CACHE_TTL_MULTIPLIER = 3;
    private TbAggLatestTelemetryNodeV2Configuration config;
    private final ConcurrentMap<EntityId, AggDeduplicationData> lastMsgMap = new ConcurrentHashMap<>();
    private final Set<String> clientAttributeNames = new HashSet<>();
    private final Set<String> sharedAttributeNames = new HashSet<>();
    private final Set<String> serverAttributeNames = new HashSet<>();
    private final Set<String> latestTsKeyNames = new HashSet<>();
    private final Map<String, ScriptEngine> attributesScriptEngineMap = new HashMap<>();
    private String queueName;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbAggLatestTelemetryNodeV2Configuration.class);
        queueName = ctx.getQueueName();
        config.getAggMappings().forEach(mapping -> {
            var filter = mapping.getFilter();
            if (filter != null) {
                addAllSafe(clientAttributeNames, filter.getClientAttributeNames());
                addAllSafe(sharedAttributeNames, filter.getSharedAttributeNames());
                addAllSafe(serverAttributeNames, filter.getServerAttributeNames());
                addAllSafe(latestTsKeyNames, filter.getLatestTsKeyNames());
                String script = filter.getTbelFilterFunction();
                attributesScriptEngineMap.put(script, ctx.getPeContext().createAttributesScriptEngine(ScriptLanguage.TBEL, script));
            }
            switch (mapping.getSourceScope()) {
                case DataConstants.CLIENT_SCOPE:
                    clientAttributeNames.add(mapping.getSource());
                    break;
                case DataConstants.SHARED_SCOPE:
                    sharedAttributeNames.add(mapping.getSource());
                    break;
                case DataConstants.SERVER_SCOPE:
                    serverAttributeNames.add(mapping.getSource());
                    break;
                case "LATEST_TELEMETRY":
                    latestTsKeyNames.add(mapping.getSource());
                    break;
            }
        });
        scheduleClearInactiveEntitiesMsg(ctx);
    }

    private void addAllSafe(Set<String> dest, Collection<String> source) {
        if (source != null && !source.isEmpty()) {
            dest.addAll(source);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        switch (msg.getInternalType()) {
            case TB_AGG_LATEST_SELF_MSG:
                processDelayedMsg(ctx, msg.getOriginator());
                break;
            case TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG:
                processClearInactiveEntitiesMsg(ctx);
                break;
            default:
                processRegularMsg(ctx, msg);
                break;
        }
    }

    private void processRegularMsg(TbContext ctx, TbMsg msg) {
        log.debug("[{}] Processing msg for entity: [{}]", ctx.getSelfId(), msg.getOriginator());
        var ts = System.currentTimeMillis();
        var entityId = msg.getOriginator();
        var deduplicationData = lastMsgMap.get(entityId);
        if (deduplicationData == null) {
            doCalculate(ctx, msg, ts);
        } else if (deduplicationData.getMsg() == null) {
            var nextCalcTs = deduplicationData.getTs() + config.getDeduplicationInSec() * 1000;
            if (nextCalcTs > ts) {
                lastMsgMap.put(entityId, new AggDeduplicationData(deduplicationData.getTs(), msg));
                scheduleDelayedMsg(ctx, entityId, nextCalcTs - ts);
                ctx.ack(msg);
            } else {
                doCalculate(ctx, msg, ts);
            }
        } else {
            lastMsgMap.put(entityId, new AggDeduplicationData(deduplicationData.getTs(), msg));
            ctx.ack(msg);
        }
    }

    private void processDelayedMsg(TbContext ctx, EntityId entityId) {
        AggDeduplicationData deduplicationData = lastMsgMap.get(entityId);
        if (deduplicationData != null && deduplicationData.getMsg() != null) {
            doCalculate(ctx, deduplicationData.getMsg(), System.currentTimeMillis());
        }
    }

    private void processClearInactiveEntitiesMsg(TbContext ctx) {
        try {
            long inactiveTs = System.currentTimeMillis() - config.getDeduplicationInSec() * 1000 * CACHE_TTL_MULTIPLIER;
            lastMsgMap.entrySet()
                    .removeIf(entry -> entry.getValue() != null
                            && entry.getValue().getTs() < inactiveTs);
        } finally {
            scheduleClearInactiveEntitiesMsg(ctx);
        }
    }

    private void scheduleClearInactiveEntitiesMsg(TbContext ctx) {
        long delayMs = Math.max(TimeUnit.HOURS.toMillis(1), config.getDeduplicationInSec() * 1000 * CACHE_TTL_MULTIPLIER);
        ctx.tellSelf(ctx.newMsg(null, TbMsgType.TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG, ctx.getSelfId(), null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING),
                delayMs);
    }

    private void doCalculate(TbContext ctx, TbMsg msg, long ts) {
        lastMsgMap.put(msg.getOriginator(), new AggDeduplicationData(ts, null));
        ListenableFuture<List<EntityId>> entityIds;
        if (EntitySearchDirection.TO.equals(config.getDirection())) {
            var relations = ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON);
            entityIds = Futures.transform(relations, r -> r.stream().map(EntityRelation::getFrom).toList(), directExecutor());
        } else {
            var relations = ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON);
            entityIds = Futures.transform(relations, r -> r.stream().map(EntityRelation::getTo).toList(), directExecutor());
        }

        ListenableFuture<List<TbAggEntityData>> entityDataList = Futures.transform(entityIds, eIds -> eIds.stream().map(entityId -> {
            TbAggEntityData data = new TbAggEntityData(entityId);
            if (!latestTsKeyNames.isEmpty()) {
                data.setTsFuture(ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, latestTsKeyNames));
            }
            if (!clientAttributeNames.isEmpty()) {
                data.setClientAttributesFuture(ctx.getAttributesService().find(ctx.getTenantId(), entityId, AttributeScope.CLIENT_SCOPE, clientAttributeNames));
            }
            if (!sharedAttributeNames.isEmpty()) {
                data.setSharedAttributesFuture(ctx.getAttributesService().find(ctx.getTenantId(), entityId, AttributeScope.SHARED_SCOPE, sharedAttributeNames));
            }
            if (!serverAttributeNames.isEmpty()) {
                data.setServerAttributesFuture(ctx.getAttributesService().find(ctx.getTenantId(), entityId, AttributeScope.SERVER_SCOPE, serverAttributeNames));
            }
            return data;
        }).toList(), ctx.getDbCallbackExecutor());

        ListenableFuture<?> allDataFuture = Futures.transformAsync(entityDataList, tmp -> {
            List<ListenableFuture<?>> futures = new ArrayList<>();
            for (var entityData : tmp) {
                if (entityData.getClientAttributesFuture() != null) {
                    futures.add(entityData.getClientAttributesFuture());
                }
                if (entityData.getSharedAttributesFuture() != null) {
                    futures.add(entityData.getSharedAttributesFuture());
                }
                if (entityData.getServerAttributesFuture() != null) {
                    futures.add(entityData.getServerAttributesFuture());
                }
                if (entityData.getTsFuture() != null) {
                    futures.add(entityData.getTsFuture());
                }
            }
            return Futures.allAsList(futures);
        }, ctx.getDbCallbackExecutor());

        Futures.addCallback(allDataFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {
                try {
                    doCalculate(ctx, msg, ts, entityDataList.get());
                } catch (Exception e) {
                    log.warn("[{}] Unexpected error: {}", ctx.getSelfId(), msg.getOriginator(), e);
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private void doCalculate(TbContext ctx, TbMsg msg, long ts, List<TbAggEntityData> childDataList) {
        if (childDataList.isEmpty()) {
            ctx.ack(msg);
            return;
        }
        childDataList.forEach(TbAggEntityData::prepare);
        JsonObject result = new JsonObject();
        for (var aggMapping : config.getAggMappings()) {
            var filteredDataList = childDataList.stream().filter(ed -> aggMapping.getFilter() == null || filter(aggMapping.getFilter().getTbelFilterFunction(), ed)).toList();
            TbAggFunction aggregation = TbAggFunctionFactory.createAggFunction(aggMapping.getAggFunction());
            filteredDataList.forEach(childData -> {
                aggregation.update(childData.getValue(aggMapping.getSourceScope(), aggMapping.getSource()), aggMapping.getDefaultValue());
            });
            aggregation.result().ifPresent(aggResult -> result.add(aggMapping.getTarget(), aggResult));
        }
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", Long.toString(ts));
        ctx.enqueueForTellNext(TbMsg.newMsg()
                .queueName(queueName)
                .type(config.getOutMsgType())
                .originator(msg.getOriginator())
                .copyMetaData(metaData)
                .data(gson.toJson(result))
                .build(), TbNodeConnectionType.SUCCESS);
        ctx.ack(msg);
    }

    @SneakyThrows
    private boolean filter(String script, TbAggEntityData ed) {
        // We use TBEL scripts only, so it is ok to do a blocking call.
        return attributesScriptEngineMap.get(script).executeAttributesFilterAsync(ed.getFilterMap()).get();
    }

    private void scheduleDelayedMsg(TbContext ctx, EntityId entityId, long delayMs) {
        ctx.tellSelf(ctx.newMsg(null, TbMsgType.TB_AGG_LATEST_SELF_MSG, entityId, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING), delayMs);
    }

    @Override
    public void destroy() {
        lastMsgMap.clear();
        clientAttributeNames.clear();
        sharedAttributeNames.clear();
        serverAttributeNames.clear();
        latestTsKeyNames.clear();

        attributesScriptEngineMap.values().forEach(scriptEngine -> {
            try {
                scriptEngine.destroy();
            } catch (Exception e) {
                log.warn("Failed to destroy script engine", e);
            }
        });
        attributesScriptEngineMap.clear();
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (oldConfiguration.has(QUEUE_NAME)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).remove(QUEUE_NAME);
                }
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
