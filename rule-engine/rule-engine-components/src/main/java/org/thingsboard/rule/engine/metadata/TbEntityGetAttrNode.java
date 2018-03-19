/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

public abstract class TbEntityGetAttrNode<T extends EntityId> implements TbNode {

    private TbGetEntityAttrNodeConfiguration config;

    @Override
    public void init(TbNodeConfiguration configuration, TbNodeState state) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetEntityAttrNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            withCallback(
                    findEntityAsync(ctx, msg.getOriginator()),
                    entityId -> withCallback(
                            ctx.getAttributesService().find(entityId, SERVER_SCOPE, config.getAttrMapping().keySet()),
                            attributes -> putAttributesAndTell(ctx, msg, attributes),
                            t -> ctx.tellError(msg, t)
                    ),
                    t -> ctx.tellError(msg, t));
        } catch (Throwable th) {
            ctx.tellError(msg, th);
        }
    }

    private void putAttributesAndTell(TbContext ctx, TbMsg msg, List<AttributeKvEntry> attributes) {
        attributes.forEach(r -> {
            String attrName = config.getAttrMapping().get(r.getKey());
            msg.getMetaData().putValue(attrName, r.getValueAsString());
        });
        ctx.tellNext(msg);
    }

    @Override
    public void destroy() {

    }

    protected abstract ListenableFuture<T> findEntityAsync(TbContext ctx, EntityId originator);

}
