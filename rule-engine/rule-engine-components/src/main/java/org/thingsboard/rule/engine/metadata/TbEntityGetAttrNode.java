/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

@Slf4j
public abstract class TbEntityGetAttrNode<T extends EntityId> implements TbNode {

    private TbGetEntityAttrNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetEntityAttrNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            withCallback(findEntityAsync(ctx, msg.getOriginator()),
                    entityId -> safeGetAttributes(ctx, msg, entityId),
                    t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } catch (Throwable th) {
            ctx.tellFailure(msg, th);
        }
    }

    private void safeGetAttributes(TbContext ctx, TbMsg msg, T entityId) {
        if (entityId == null || entityId.isNullUid()) {
            ctx.tellNext(msg, FAILURE);
            return;
        }

        withCallback(config.isTelemetry() ? getLatestTelemetry(ctx, entityId) : getAttributesAsync(ctx, entityId),
                attributes -> putAttributesAndTell(ctx, msg, attributes),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<List<KvEntry>> getAttributesAsync(TbContext ctx, EntityId entityId) {
        ListenableFuture<List<AttributeKvEntry>> latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, SERVER_SCOPE, config.getAttrMapping().keySet());
        return Futures.transform(latest, l ->
                l.stream().map(i -> (KvEntry) i).collect(Collectors.toList()), MoreExecutors.directExecutor());
    }

    private ListenableFuture<List<KvEntry>> getLatestTelemetry(TbContext ctx, EntityId entityId) {
        ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, config.getAttrMapping().keySet());
        return Futures.transform(latest, l ->
                l.stream().map(i -> (KvEntry) i).collect(Collectors.toList()), MoreExecutors.directExecutor());
    }


    private void putAttributesAndTell(TbContext ctx, TbMsg msg, List<? extends KvEntry> attributes) {
        attributes.forEach(r -> {
            String attrName = config.getAttrMapping().get(r.getKey());
            msg.getMetaData().putValue(attrName, r.getValueAsString());
        });
        ctx.tellSuccess(msg);
    }

    @Override
    public void destroy() {

    }

    protected abstract ListenableFuture<T> findEntityAsync(TbContext ctx, EntityId originator);

    public void setConfig(TbGetEntityAttrNodeConfiguration config) {
        this.config = config;
    }

}
