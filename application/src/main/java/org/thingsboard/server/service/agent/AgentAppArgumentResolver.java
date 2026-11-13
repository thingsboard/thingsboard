// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppArgument;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentValueType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.agent.AgentAppArgumentSourceResolver;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the user-defined dynamic arguments of an agent application's config to concrete string values.
 * Lookups are batched per source entity: a single latest-telemetry call and one attribute call per scope
 * for each distinct source entity, instead of one call per argument.
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class AgentAppArgumentResolver {

    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private final AgentAppArgumentSourceResolver sourceEntityResolver;

    /**
     * Never throws: an argument that cannot be resolved on the calling thread is reported through the returned
     * future, so callers can handle every failure with a single callback.
     */
    public ListenableFuture<Map<String, String>> resolve(TenantId tenantId, AgentApplication application) {
        try {
            return doResolve(tenantId, application);
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<Map<String, String>> doResolve(TenantId tenantId, AgentApplication application) {
        AgentAppConfig config = application.getConfig();
        if (config == null || CollectionsUtil.isEmpty(config.getArguments())) {
            return Futures.immediateFuture(Collections.emptyMap());
        }

        Map<AgentAppArgumentSource, EntityId> contextSourceCache = new EnumMap<>(AgentAppArgumentSource.class);
        Map<EntityId, List<AgentAppArgument>> argsByEntity = new LinkedHashMap<>();
        Map<String, String> baseValues = new HashMap<>();

        for (AgentAppArgument argument : config.getArguments()) {
            AgentAppArgumentSource sourceType = argument.getSourceType();
            EntityId sourceEntity = sourceType != null && !sourceType.isConcreteEntityRef()
                    ? contextSourceCache.computeIfAbsent(sourceType, type -> sourceEntityResolver.resolveContextSource(tenantId, application, type))
                    : argument.getSourceEntityId();
            if (sourceEntity == null) {
                baseValues.put(argument.getName(), resolveValue(null, argument));
            } else {
                argsByEntity.computeIfAbsent(sourceEntity, k -> new ArrayList<>()).add(argument);
            }
        }

        List<ListenableFuture<Map<String, String>>> futures = new ArrayList<>();
        argsByEntity.forEach((entityId, entityArgs) ->
                futures.addAll(fetchForEntity(tenantId, entityId, entityArgs)));

        if (futures.isEmpty()) {
            return Futures.immediateFuture(baseValues);
        }
        return Futures.transform(Futures.allAsList(futures), resolvedKvs -> {
            Map<String, String> result = new HashMap<>(baseValues);
            resolvedKvs.forEach(result::putAll);
            return result;
        }, MoreExecutors.directExecutor());
    }

    private List<ListenableFuture<Map<String, String>>> fetchForEntity(TenantId tenantId, EntityId entityId,
                                                                        List<AgentAppArgument> entityArgs) {
        List<ListenableFuture<Map<String, String>>> futures = new ArrayList<>();

        List<AgentAppArgument> telemetryArgs = entityArgs.stream()
                .filter(arg -> arg.getValueType() == AgentAppArgumentValueType.LATEST_TELEMETRY)
                .toList();

        if (!telemetryArgs.isEmpty()) {
            futures.add(fetchAndMapLatestTelemetries(tenantId, entityId, telemetryArgs));
        }

        Map<AttributeScope, List<AgentAppArgument>> attributeArgsByScope = entityArgs.stream()
                .filter(arg -> arg.getValueType() == AgentAppArgumentValueType.ATTRIBUTE)
                .collect(Collectors.groupingBy(AgentAppArgument::getScopeOrDefault));

        attributeArgsByScope.forEach((scope, scopeArgs) ->
                futures.add(fetchAndMapAttributes(tenantId, entityId, scope, scopeArgs)));

        return futures;
    }

    private ListenableFuture<Map<String, String>> fetchAndMapAttributes(TenantId tenantId, EntityId entityId, AttributeScope scope, List<AgentAppArgument> scopeArgs) {
        Set<String> keys = scopeArgs.stream().map(AgentAppArgument::getKey).collect(Collectors.toSet());
        ListenableFuture<List<AttributeKvEntry>> attrFuture = attributesService.find(tenantId, entityId, scope, keys);

        return Futures.transform(attrFuture, entries -> {
            Map<String, String> values = toValueMap(entries, AttributeKvEntry::getKey);
            return mapArguments(scopeArgs, values);
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Map<String, String>> fetchAndMapLatestTelemetries(TenantId tenantId, EntityId entityId, List<AgentAppArgument> telemetryArgs) {
        Set<String> keys = telemetryArgs.stream().map(AgentAppArgument::getKey).collect(Collectors.toSet());
        ListenableFuture<List<TsKvEntry>> tsFuture = timeseriesService.findLatest(tenantId, entityId, keys);
        return Futures.transform(tsFuture, entries -> {
            Map<String, String> values = toValueMap(entries, TsKvEntry::getKey);
            return mapArguments(telemetryArgs, values);
        }, MoreExecutors.directExecutor());
    }

    private <T extends KvEntry> Map<String, String> toValueMap(List<T> entries, Function<T, String> keyFn) {
        return entries.stream()
                .filter(entry -> entry.getValueAsString() != null)
                .collect(Collectors.toMap(keyFn, KvEntry::getValueAsString));
    }

    private Map<String, String> mapArguments(List<AgentAppArgument> args, Map<String, String> fetchedValues) {
        return args.stream().collect(Collectors.toMap(
                AgentAppArgument::getName,
                arg -> resolveValue(fetchedValues.get(arg.getKey()), arg)));
    }

    private String resolveValue(String fetched, AgentAppArgument argument) {
        if (fetched != null) {
            return fetched;
        }
        if (argument.getDefaultValue() != null) {
            return argument.getDefaultValue();
        }
        throw new IllegalStateException("No value resolved for agent app argument '" + argument.getName()
                + "' (source " + argument.getSourceType() + ", key '" + argument.getKey()
                + "') and no default value is set");
    }

}
