// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNode;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "aggregate latest (deprecated)",
        configClazz = TbAggLatestTelemetryNodeConfiguration.class,
        version = 2,
        hasQueueName = true,
        nodeDescription = "Periodically aggregates entities attributes or latest timeseries",
        nodeDetails = "Performs aggregation of attributes or latest timeseries fetched from child entities with configurable period. " +
                "Generates outgoing messages with aggregated values for each parent entity. By default, an outgoing message generates with 'POST_TELEMETRY_REQUEST' type. " +
                "The type of the outgoing messages controls under \"<b>Output message type</b>\" configuration parameter.",
        inEnabled = false,
        configDirective = "tbAnalyticsNodeAggregateLatestConfig",
        icon = "functions"
)
public class TbAggLatestTelemetryNode extends TbAbstractLatestNode<TbAggLatestTelemetryNodeConfiguration> {

    private final ConcurrentMap<String, ScriptEngine> attributesScriptEngineMap = new ConcurrentHashMap<>();

    @Override
    protected TbAggLatestTelemetryNodeConfiguration loadMapperNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAggLatestTelemetryNodeConfiguration.class);
    }

    @Override
    protected TbMsgType tickMessageType() {
        return TbMsgType.TB_AGG_LATEST_SELF_MSG;
    }

    @Override
    protected Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> doParentAggregations(TbContext ctx, EntityId parentEntityId) {
        ListenableFuture<List<EntityId>> childEntityIds = config.getParentEntitiesQuery().getChildEntitiesAsync(ctx, parentEntityId);
        List<ListenableFuture<Optional<JsonObject>>> aggregateFutures = new ArrayList<>();
        config.getAggMappings().forEach(aggMapping -> aggregateFutures.add(aggMapping.aggregate(ctx, attributesScriptEngineMap, childEntityIds)));
        Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> result = new HashMap<>();
        result.put(parentEntityId, aggregateFutures);
        return result;
    }

    @Override
    public void destroy() {
        super.destroy();
        for (ScriptEngine se : attributesScriptEngineMap.values()) {
            try {
                se.destroy();
            } catch (Exception e) {
                log.warn("Failed to destroy script engine", e);
            }
        }
        attributesScriptEngineMap.clear();
    }

}
