/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.LATEST_TS;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;

public abstract class TbAbstractGetAttributesNode<C extends TbGetAttributesNodeConfiguration, T extends EntityId> implements TbNode {

    private static ObjectMapper mapper = new ObjectMapper();

    private static final String VALUE = "value";
    private static final String TS = "ts";

    protected C config;
    private boolean fetchToData;
    private boolean isTellFailureIfAbsent;
    private boolean getLatestValueWithTs;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadGetAttributesNodeConfig(configuration);
        this.fetchToData = config.isFetchToData();
        this.isTellFailureIfAbsent = BooleanUtils.toBooleanDefaultIfNull(this.config.isTellFailureIfAbsent(), true);
        this.getLatestValueWithTs = BooleanUtils.toBooleanDefaultIfNull(this.config.isGetLatestValueWithTs(), false);
        mapper.configure(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature(), false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    protected abstract C loadGetAttributesNodeConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            withCallback(
                    findEntityIdAsync(ctx, msg),
                    entityId -> safePutAttributes(ctx, msg, entityId),
                    t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } catch (Throwable th) {
            ctx.tellFailure(msg, th);
        }
    }

    protected abstract ListenableFuture<T> findEntityIdAsync(TbContext ctx, TbMsg msg);

    private void safePutAttributes(TbContext ctx, TbMsg msg, T entityId) {
        if (entityId == null || entityId.isNullUid()) {
            ctx.tellNext(msg, FAILURE);
            return;
        }
        JsonNode msgDataNode = JacksonUtil.toJsonNode(msg.getData());
        if (fetchToData) {
            if (!msgDataNode.isObject()) {
                ctx.tellFailure(msg, new IllegalArgumentException("Msg body is not object!"));
                return;
            }
        }
        ConcurrentHashMap<String, List<String>> failuresMap = new ConcurrentHashMap<>();
        ListenableFuture<List<Map<String, List<? extends KvEntry>>>> allFutures = Futures.allAsList(
                getLatestTelemetry(ctx, entityId, msg, LATEST_TS, TbNodeUtils.processPatterns(config.getLatestTsKeyNames(), msg), failuresMap),
                getAttrAsync(ctx, entityId, CLIENT_SCOPE, TbNodeUtils.processPatterns(config.getClientAttributeNames(), msg), failuresMap),
                getAttrAsync(ctx, entityId, SHARED_SCOPE, TbNodeUtils.processPatterns(config.getSharedAttributeNames(), msg), failuresMap),
                getAttrAsync(ctx, entityId, SERVER_SCOPE, TbNodeUtils.processPatterns(config.getServerAttributeNames(), msg), failuresMap)
        );
        withCallback(allFutures, futuresList -> {
            if (!failuresMap.isEmpty()) {
                throw reportFailures(failuresMap);
            }
            TbMsgMetaData msgMetaData = msg.getMetaData();
            futuresList.stream().filter(Objects::nonNull).forEach(kvEntriesMap -> {
                kvEntriesMap.forEach((keyScope, kvEntryList) -> {
                    String prefix = getPrefix(keyScope);
                    kvEntryList.forEach(kvEntry -> {
                        if (fetchToData) {
                            JacksonUtil.addKvEntry((ObjectNode) msgDataNode, kvEntry, prefix);
                        } else {
                            msgMetaData.putValue(prefix + kvEntry.getKey(), kvEntry.getValueAsString());
                        }
                    });
                });
            });
            if (fetchToData) {
                ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), JacksonUtil.toString(msgDataNode)));
            } else {
                ctx.tellSuccess(msg);
            }
        }, t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Map<String, List<? extends KvEntry>>> getAttrAsync(TbContext ctx, EntityId entityId, String scope, List<String> keys, ConcurrentHashMap<String, List<String>> failuresMap) {
        if (CollectionUtils.isEmpty(keys)) {
            return Futures.immediateFuture(null);
        }
        ListenableFuture<List<AttributeKvEntry>> attributeKvEntryListFuture = ctx.getAttributesService().find(ctx.getTenantId(), entityId, scope, keys);
        return Futures.transform(attributeKvEntryListFuture, attributeKvEntryList -> {
            if (isTellFailureIfAbsent && attributeKvEntryList.size() != keys.size()) {
                getNotExistingKeys(attributeKvEntryList, keys).forEach(key -> computeFailuresMap(scope, failuresMap, key));
            }
            Map<String, List<? extends KvEntry>> mapAttributeKvEntry = new HashMap<>();
            mapAttributeKvEntry.put(scope, attributeKvEntryList);
            return mapAttributeKvEntry;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Map<String, List<? extends KvEntry>>> getLatestTelemetry(TbContext ctx, EntityId entityId, TbMsg msg, String scope, List<String> keys, ConcurrentHashMap<String, List<String>> failuresMap) {
        if (CollectionUtils.isEmpty(keys)) {
            return Futures.immediateFuture(null);
        }
        ListenableFuture<List<TsKvEntry>> latestTelemetryFutures = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, keys);
        return Futures.transform(latestTelemetryFutures, tsKvEntries -> {
            List<TsKvEntry> listTsKvEntry = new ArrayList<>();
            tsKvEntries.forEach(tsKvEntry -> {
                if (tsKvEntry.getValue() == null) {
                    if (isTellFailureIfAbsent) {
                        computeFailuresMap(scope, failuresMap, tsKvEntry.getKey());
                    }
                } else if (getLatestValueWithTs) {
                    listTsKvEntry.add(getValueWithTs(tsKvEntry));
                } else {
                    listTsKvEntry.add(new BasicTsKvEntry(tsKvEntry.getTs(), tsKvEntry));
                }
            });
            Map<String, List<? extends KvEntry>> mapTsKvEntry = new HashMap<>();
            mapTsKvEntry.put(scope, listTsKvEntry);
            return mapTsKvEntry;
        }, MoreExecutors.directExecutor());
    }

    private TsKvEntry getValueWithTs(TsKvEntry tsKvEntry) {
        ObjectNode value = JacksonUtil.newObjectNode();
        value.put(TS, tsKvEntry.getTs());
        switch (tsKvEntry.getDataType()) {
            case STRING:
                value.put(VALUE, tsKvEntry.getValueAsString());
                break;
            case LONG:
                value.put(VALUE, tsKvEntry.getLongValue().get());
                break;
            case BOOLEAN:
                value.put(VALUE, tsKvEntry.getBooleanValue().get());
                break;
            case DOUBLE:
                value.put(VALUE, tsKvEntry.getDoubleValue().get());
                break;
            case JSON:
                try {
                    value.set(VALUE, mapper.readTree(tsKvEntry.getJsonValue().get()));
                } catch (IOException e) {
                    throw new JsonParseException("Can't parse jsonValue: " + tsKvEntry.getJsonValue().get(), e);
                }
                break;
        }
        return new BasicTsKvEntry(tsKvEntry.getTs(), new JsonDataEntry(tsKvEntry.getKey(), value.toString()));
    }

    private String getPrefix(String scope) {
        String prefix = "";
        switch (scope) {
            case CLIENT_SCOPE:
                prefix = "cs_";
                break;
            case SHARED_SCOPE:
                prefix = "shared_";
                break;
            case SERVER_SCOPE:
                prefix = "ss_";
                break;
        }
        return prefix;
    }

    private List<String> getNotExistingKeys(List<AttributeKvEntry> existingAttributesKvEntry, List<String> allKeys) {
        List<String> existingKeys = existingAttributesKvEntry.stream().map(KvEntry::getKey).collect(Collectors.toList());
        return allKeys.stream().filter(key -> !existingKeys.contains(key)).collect(Collectors.toList());
    }

    private void computeFailuresMap(String scope, ConcurrentHashMap<String, List<String>> failuresMap, String key) {
        List<String> failures = failuresMap.computeIfAbsent(scope, k -> new ArrayList<>());
        failures.add(key);
    }

    private RuntimeException reportFailures(ConcurrentHashMap<String, List<String>> failuresMap) {
        StringBuilder errorMessage = new StringBuilder("The following attribute/telemetry keys is not present in the DB: ").append("\n");
        if (failuresMap.containsKey(CLIENT_SCOPE)) {
            errorMessage.append("\t").append("[" + CLIENT_SCOPE + "]:").append(failuresMap.get(CLIENT_SCOPE).toString()).append("\n");
        }
        if (failuresMap.containsKey(SERVER_SCOPE)) {
            errorMessage.append("\t").append("[" + SERVER_SCOPE + "]:").append(failuresMap.get(SERVER_SCOPE).toString()).append("\n");
        }
        if (failuresMap.containsKey(SHARED_SCOPE)) {
            errorMessage.append("\t").append("[" + SHARED_SCOPE + "]:").append(failuresMap.get(SHARED_SCOPE).toString()).append("\n");
        }
        if (failuresMap.containsKey(LATEST_TS)) {
            errorMessage.append("\t").append("[" + LATEST_TS + "]:").append(failuresMap.get(LATEST_TS).toString()).append("\n");
        }
        failuresMap.clear();
        return new RuntimeException(errorMessage.toString());
    }
}
