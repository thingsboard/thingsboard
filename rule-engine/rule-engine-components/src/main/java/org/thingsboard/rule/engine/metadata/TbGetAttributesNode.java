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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.util.List;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
public class TbGetAttributesNode implements TbNode {

    TbGetAttributesNodeConfiguration config;

    @Override
    public void init(TbNodeConfiguration configuration, TbNodeState state) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetAttributesNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            //TODO: refactor this to work async and fetch attributes from cache.
            AttributesService service = ctx.getAttributesService();
            fetchAttributes(msg, service, config.getClientAttributeNames(), DataConstants.CLIENT_SCOPE, "cs.");
            fetchAttributes(msg, service, config.getServerAttributeNames(), DataConstants.SERVER_SCOPE, "ss.");
            fetchAttributes(msg, service, config.getSharedAttributeNames(), DataConstants.SHARED_SCOPE, "shared.");
            ctx.tellNext(msg);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to fetch attributes", msg.getOriginator(), msg.getId(), e);
            throw new TbNodeException(e);
        }
    }

    private void fetchAttributes(TbMsg msg, AttributesService service, List<String> attributeNames, String scope, String prefix) throws InterruptedException, java.util.concurrent.ExecutionException {
        if (attributeNames != null && attributeNames.isEmpty()) {
            List<AttributeKvEntry> attributes = service.find(msg.getOriginator(), scope, attributeNames).get();
            attributes.forEach(attr -> msg.getMetaData().putValue(prefix + attr.getKey(), attr.getValueAsString()));
        }
    }

    @Override
    public void destroy() {

    }
}
