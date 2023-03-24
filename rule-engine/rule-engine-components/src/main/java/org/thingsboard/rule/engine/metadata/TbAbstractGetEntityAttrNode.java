/**
 * Copyright © 2016-2023 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

@Slf4j
public abstract class TbAbstractGetEntityAttrNode<T extends EntityId> extends TbAbstractNodeWithFetchTo<TbGetEntityAttrNodeConfiguration> {
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ObjectNode msgDataAsJsonNode;
        if (FetchTo.DATA.equals(fetchTo)) {
            msgDataAsJsonNode = getMsgDataAsObjectNode(msg);
        } else {
            msgDataAsJsonNode = null;
        }
        ctx.checkTenantEntity(msg.getOriginator());
        withCallback(findEntityAsync(ctx, msg.getOriginator()),
                entityId -> safeGetAttributes(ctx, msg, entityId, msgDataAsJsonNode),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract ListenableFuture<T> findEntityAsync(TbContext ctx, EntityId originator);

    private void safeGetAttributes(TbContext ctx, TbMsg msg, T entityId, ObjectNode msgDataAsJsonNode) {
        if (entityId == null || entityId.isNullUid()) {
            ctx.tellFailure(msg, new NoSuchElementException("Did not find entity! Msg ID: " + msg.getId()));
            return;
        }

        Map<String, String> mappingsMap = new HashMap<>();
        config.getAttrMapping().forEach((key, value) -> {
            String patternProcessedSourceKey = TbNodeUtils.processPattern(key, msg);
            String patternProcessedTargetKey = TbNodeUtils.processPattern(value, msg);
            mappingsMap.put(patternProcessedSourceKey, patternProcessedTargetKey);
        });

        var sourceKeys = List.copyOf(mappingsMap.keySet());
        withCallback(config.isTelemetry() ? getLatestTelemetryAsync(ctx, entityId, sourceKeys) : getAttributesAsync(ctx, entityId, sourceKeys),
                data -> putDataAndTell(ctx, msg, data, mappingsMap, msgDataAsJsonNode),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<List<KvEntry>> getAttributesAsync(TbContext ctx, EntityId entityId, List<String> attrKeys) {
        var latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, SERVER_SCOPE, attrKeys);
        return Futures.transform(latest, l ->
                        l.stream()
                                .map(i -> (KvEntry) i)
                                .collect(Collectors.toList()),
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<List<KvEntry>> getLatestTelemetryAsync(TbContext ctx, EntityId entityId, List<String> timeseriesKeys) {
        var latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, timeseriesKeys);
        return Futures.transform(latest, l ->
                        l.stream()
                                .map(i -> (KvEntry) i)
                                .collect(Collectors.toList()),
                MoreExecutors.directExecutor());
    }

    private void putDataAndTell(TbContext ctx, TbMsg msg, List<? extends KvEntry> data, Map<String, String> map, ObjectNode msgDataAsJsonNode) {
        for (KvEntry entry : data) {
            String targetKey = map.get(entry.getKey());
            String value = entry.getValueAsString();
            if (FetchTo.DATA.equals(fetchTo)) {
                msgDataAsJsonNode.put(targetKey, value);
            } else if (FetchTo.METADATA.equals(fetchTo)) {
                msg.getMetaData().putValue(targetKey, value);
            }
        }
        if (FetchTo.DATA.equals(fetchTo)) {
            ctx.tellSuccess(TbMsg.transformMsgData(msg, JacksonUtil.toString(msgDataAsJsonNode)));
        } else if (FetchTo.METADATA.equals(fetchTo)) {
            ctx.tellSuccess(msg);
        }
    }
}
