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
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "split array msg",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Split array message into several msgs",
        nodeDetails = "",
        icon = "functions",
        configDirective = "tbNodeEmptyConfig"
)
public class TbSplitArrayMsgNode implements TbNode {

    EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        JsonNode jsonNode = JacksonUtil.toJsonNode(msg.getData());
        if (jsonNode.isArray()) {
            ArrayNode data = (ArrayNode) jsonNode;
            List<TbMsg> messages = new ArrayList<>();
            data.forEach(msgNode -> {
                messages.add(createNewMsg(msg, msgNode));
            });
            ctx.ack(msg);
            if (messages.size() == 1) {
                ctx.tellSuccess(messages.get(0));
            } else {
                for (TbMsg newMsg : messages) {
                    ctx.tellSuccess(newMsg);
                }
            }
        } else {
            ctx.tellSuccess(msg);
        }
    }

    private TbMsg createNewMsg(TbMsg msg, JsonNode msgNode) {
        return TbMsg.newMsg(msg.getQueueName(), msg.getType(), msg.getOriginator(), msg.getMetaData(), JacksonUtil.toString(msgNode));
    }

    @Override
    public void destroy() {

    }
}
