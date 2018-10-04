/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

import static org.thingsboard.rule.engine.api.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;

public abstract class TbAbstractGetAttributesNode<C extends TbGetAttributesNodeConfiguration, T extends EntityId> implements TbNode {

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadGetAttributesNodeConfig(configuration);
    }

    protected abstract C loadGetAttributesNodeConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            withCallback(
                    findEntityIdAsync(ctx, msg.getOriginator()),
                    entityId -> safePutAttributes(ctx, msg, entityId),
                    t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } catch (Throwable th) {
            ctx.tellFailure(msg, th);
        }
    }

    private void safePutAttributes(TbContext ctx, TbMsg msg, T entityId) {
        if (entityId == null || entityId.isNullUid()) {
            ctx.tellNext(msg, FAILURE);
            return;
        }
        ListenableFuture<List<Void>> allFutures = Futures.allAsList(
                putLatestTelemetry(ctx, entityId, msg, config.getLatestTsKeyNames()),
                putAttrAsync(ctx, entityId, msg, CLIENT_SCOPE, config.getClientAttributeNames(), "cs_"),
                putAttrAsync(ctx, entityId, msg, SHARED_SCOPE, config.getSharedAttributeNames(), "shared_"),
                putAttrAsync(ctx, entityId, msg, SERVER_SCOPE, config.getServerAttributeNames(), "ss_")
        );
        withCallback(allFutures, i -> ctx.tellNext(msg, SUCCESS), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Void> putAttrAsync(TbContext ctx, EntityId entityId, TbMsg msg, String scope, List<String> keys, String prefix) {
        if (CollectionUtils.isEmpty(keys)) {
            return Futures.immediateFuture(null);
        }
        ListenableFuture<List<AttributeKvEntry>> latest = ctx.getAttributesService().find(entityId, scope, keys);
        return Futures.transform(latest, l -> {
            l.forEach(r -> {
                if (r.getValue() != null) {
                    msg.getMetaData().putValue(prefix + r.getKey(), r.getValueAsString());
                } else {
                    throw new RuntimeException("[" + scope + "][" + r.getKey() + "] attribute value is not present in the DB!");
                }
            });
            return null;
        });
    }

    private ListenableFuture<Void> putLatestTelemetry(TbContext ctx, EntityId entityId, TbMsg msg, List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Futures.immediateFuture(null);
        }
        ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(entityId, keys);
        return Futures.transform(latest, l -> {
            l.forEach(r -> {
                if (r.getValue() != null) {
                    msg.getMetaData().putValue(r.getKey(), r.getValueAsString());
                } else {
                    throw new RuntimeException("[" + r.getKey() + "] telemetry value is not present in the DB!");
                }
            });
            return null;
        });
    }

    @Override
    public void destroy() {

    }

    protected abstract ListenableFuture<T> findEntityIdAsync(TbContext ctx, EntityId originator);
}
