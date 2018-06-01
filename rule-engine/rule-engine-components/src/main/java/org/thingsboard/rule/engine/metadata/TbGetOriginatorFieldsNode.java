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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesFieldsAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.rule.engine.api.util.DonAsynchron.withCallback;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "entity fields",
        configClazz = TbGetOriginatorFieldsConfiguration.class,
        nodeDescription = "Add Message Originator Name and Type into Message Metadata",
        nodeDetails = "If originator is Asset, Device or Alarm, both name and type are added. In all other cases type will always be \"default\"")
public class TbGetOriginatorFieldsNode implements TbNode {

    private TbGetOriginatorFieldsConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbGetOriginatorFieldsConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            withCallback(putEntityFields(ctx, msg.getOriginator(), msg),
                    i -> ctx.tellNext(msg, SUCCESS), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } catch (Throwable th) {
            ctx.tellFailure(msg, th);
        }
    }

    private ListenableFuture<Void> putEntityFields(TbContext ctx, EntityId entityId, TbMsg msg) {
        if (!config.isFetchName() && !config.isFetchType()) {
            return Futures.immediateFuture(null);
        } else {
            return Futures.transform(EntitiesFieldsAsyncLoader.findAsync(ctx, entityId),
                    data -> {
                        if (config.isFetchName()) {
                            msg.getMetaData().putValue(config.getNameMetadataKey(), data.getName());
                        }
                        if (config.isFetchType()) {
                            msg.getMetaData().putValue(config.getTypeMetadataKey(), data.getType());
                        }
                        return null;
                    }
            );
        }
    }

    @Override
    public void destroy() {

    }
}
