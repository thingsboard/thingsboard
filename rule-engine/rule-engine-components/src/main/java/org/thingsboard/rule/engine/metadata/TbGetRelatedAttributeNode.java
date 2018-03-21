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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbNodeState;
import org.thingsboard.rule.engine.api.EnrichmentNode;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;

import org.thingsboard.server.common.data.id.EntityId;

@EnrichmentNode(name="Get Related Entity Attributes Node")
public class TbGetRelatedAttributeNode extends TbEntityGetAttrNode<EntityId> {

    private TbGetRelatedAttrNodeConfiguration config;

    @Override
    public void init(TbNodeConfiguration configuration, TbNodeState state) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetRelatedAttrNodeConfiguration.class);
        setConfig(config);
    }

    @Override
    protected ListenableFuture<EntityId> findEntityAsync(TbContext ctx, EntityId originator) {
        return EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, originator, config.getDirection(), config.getRelationType());
    }
}
