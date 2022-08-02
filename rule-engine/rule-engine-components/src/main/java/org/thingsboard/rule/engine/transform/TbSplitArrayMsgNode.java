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
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "split array msg",
        configClazz = TbSplitArrayMsgNodeConfiguration.class,
        nodeDescription = "Split array message into several msgs",
        nodeDetails = "Split the array fetched from the msg body by json path expression. The default value of the " +
                " expression is set to <b>$</b> (the root object/element).  If extracted field from the expression " +
                " is not found or the received field is not a json array, the  <code>Failure</code> chain is used, " +
                " otherwise returns inner objects of the extracted array as separate messages via <code>Success</code> chain",
        icon = "functions",
        configDirective = "tbTransformationNodeSplitArrayMsgConfig"
)
public class TbSplitArrayMsgNode implements TbNode {

    TbSplitArrayMsgNodeConfiguration config;
    Configuration configurationJsonPath;
    JsonPath jsonPath;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSplitArrayMsgNodeConfiguration.class);
        this.configurationJsonPath = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .build();
        if (StringUtils.isEmpty(config.getJsonPath())) {
            throw new IllegalArgumentException("JsonPath expression is not specified");
        }
        this.jsonPath = JsonPath.compile(config.getJsonPath());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        try {
            JsonNode arrayData = jsonPath.read(msg.getData(), this.configurationJsonPath);
            if (arrayData.isArray()) {
                splitArrayMsg(ctx, msg, (ArrayNode) arrayData);
            } else {
                ctx.tellFailure(msg, new RuntimeException("JsonPath expression '" + config.getJsonPath() + "' returned not a json array!"));
            }
        } catch (PathNotFoundException e) {
            ctx.tellFailure(msg, e);
        }
    }

    private void splitArrayMsg(TbContext ctx, TbMsg msg, ArrayNode data) {
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
    }

    private TbMsg createNewMsg(TbMsg msg, JsonNode msgNode) {
        return TbMsg.newMsg(msg.getQueueName(), msg.getType(), msg.getOriginator(), msg.getMetaData(), JacksonUtil.toString(msgNode));
    }

    @Override
    public void destroy() {

    }
}
