// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.script.ScriptLanguage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class AggLatestMappingFilter {

    private List<String> clientAttributeNames;
    private List<String> sharedAttributeNames;
    private List<String> serverAttributeNames;
    private List<String> latestTsKeyNames;

    private ScriptLanguage scriptLang;
    private String filterFunction;
    private String tbelFilterFunction;

    public ListenableFuture<List<EntityId>> filterEntityIds(TbContext ctx, Map<String, ScriptEngine> attributesScriptEngineMap, List<EntityId> entityIds) {
        List<ListenableFuture<Optional<EntityId>>> resultFutures = new ArrayList<>();
        entityIds.forEach(id -> {
            resultFutures.add(filter(ctx, attributesScriptEngineMap, id));
        });
        return Futures.transform(Futures.allAsList(resultFutures), results -> results.stream()
                .filter(res -> res.isPresent()).map(Optional::get).collect(Collectors.toList()), MoreExecutors.directExecutor());
    }

    private ListenableFuture<Optional<EntityId>> filter(TbContext ctx, Map<String, ScriptEngine> attributesScriptEngineMap, EntityId entityId) {
        try {
            Map<String, KvEntry> attributes = new HashMap<>();
            prepareAttributes(ctx, attributes, entityId, AttributeScope.CLIENT_SCOPE, clientAttributeNames, "cs_");
            prepareAttributes(ctx, attributes, entityId, AttributeScope.SHARED_SCOPE, sharedAttributeNames, "shared_");
            prepareAttributes(ctx, attributes, entityId, AttributeScope.SERVER_SCOPE, serverAttributeNames, "ss_");
            prepareTimeseries(ctx, attributes, entityId, latestTsKeyNames);
            String script = (scriptLang == null || ScriptLanguage.JS.equals(scriptLang)) ? filterFunction : tbelFilterFunction;
            ScriptEngine attributesScriptEngine = attributesScriptEngineMap.computeIfAbsent(script,
                    function -> ctx.getPeContext().createAttributesScriptEngine(scriptLang, function));
            return Futures.transform(attributesScriptEngine.executeAttributesFilterAsync(attributes), res ->
                    res ? Optional.of(entityId) : Optional.empty(), MoreExecutors.directExecutor());
        } catch (Exception e) {
            return Futures.immediateFailedFuture(new RuntimeException("[" + entityId + "] Failed to execute attributes mapping filter!", e));
        }
    }

    private void prepareAttributes(TbContext ctx, Map<String, KvEntry> attributes, EntityId entityId, AttributeScope scope, List<String> keys, String prefix) throws Exception {
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<AttributeKvEntry>> latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, scope, keys);
            latest.get().forEach(r -> {
                if (r.getValue() != null) {
                    attributes.put(prefix + r.getKey(), r);
                }
            });
        }
    }

    private void prepareTimeseries(TbContext ctx, Map<String, KvEntry> attributes, EntityId entityId, List<String> keys) throws Exception {
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, keys);
            latest.get().forEach(r -> {
                if (r.getValue() != null) {
                    attributes.put(r.getKey(), r);
                }
            });
        }
    }

}
