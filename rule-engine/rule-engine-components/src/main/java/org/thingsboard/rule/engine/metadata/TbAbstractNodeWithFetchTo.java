/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.msg.TbMsg;

public abstract class TbAbstractNodeWithFetchTo<C extends TbAbstractFetchToNodeConfiguration> implements TbNode {
    protected C config;
    protected FetchTo fetchTo;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = loadNodeConfiguration(configuration);
        if (config.getFetchTo() == null) {
            throw new TbNodeException("FetchTo cannot be NULL!");
        } else {
            fetchTo = config.getFetchTo();
        }
    }

    protected abstract C loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException;

    protected ObjectNode getMsgDataAsObjectNode(TbMsg msg) {
        JsonNode msgDataNode = JacksonUtil.toJsonNode(msg.getData());
        if (!msgDataNode.isObject()) {
            throw new IllegalArgumentException("Message body is not an object!");
        }
        return (ObjectNode) msgDataNode;
    }
}
