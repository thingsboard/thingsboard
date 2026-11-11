// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
public class AggLatestMapping {

    private String source;
    private String sourceScope;
    private double defaultValue;
    private String target;
    private MathFunction aggFunction;
    private AggLatestMappingFilter filter;

    public ListenableFuture<Optional<JsonObject>> aggregate(TbContext ctx, Map<String, ScriptEngine> attributesScriptEngineMap,
                                                            ListenableFuture<List<EntityId>> entityIds) {
        ListenableFuture<List<EntityId>> filteredEntityIds =
                filter != null
                        ? Futures.transformAsync(entityIds, ids -> filter.filterEntityIds(ctx, attributesScriptEngineMap, ids), ctx.getDbCallbackExecutor())
                        : entityIds;

        return Futures.transform(filteredEntityIds, ids -> {
            TbAggFunction aggregation = TbAggFunctionFactory.createAggFunction(aggFunction);
            ids.forEach(entityId -> {
                Optional<KvEntry> entry = aggregation.fetchAttrValue() ? fetchValue(ctx, entityId) : Optional.empty();
                aggregation.update(entry, defaultValue);
            });
            Optional<JsonElement> result = aggregation.result();
            if (result.isPresent()) {
                JsonObject obj = new JsonObject();
                obj.add(target, result.get());
                return Optional.of(obj);
            } else {
                return Optional.empty();
            }
        }, ctx.getDbCallbackExecutor());
    }

    private Optional<KvEntry> fetchValue(TbContext ctx, EntityId entityId) {
        try {
            if ("LATEST_TELEMETRY".equals(sourceScope)) {
                ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, Collections.singletonList(source));
                List<TsKvEntry> latestTs = latest.get();
                if (latestTs != null && !latestTs.isEmpty() && latestTs.get(0).getValue() != null) {
                    return Optional.of(latestTs.get(0));
                }
            } else {
                ListenableFuture<Optional<AttributeKvEntry>> latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, AttributeScope.valueOf(sourceScope), source);
                Optional<AttributeKvEntry> latestAttr = latest.get();
                if (latestAttr.isPresent()) {
                    return Optional.of(latestAttr.get());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch value of attribute/telemetry telemetry [" + source + "] of entity [" + entityId + "]", e);
        }
        return Optional.empty();
    }

}
