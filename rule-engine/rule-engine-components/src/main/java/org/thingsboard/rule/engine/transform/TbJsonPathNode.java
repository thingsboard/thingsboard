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

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "json path",
        configClazz = TbJsonPathNodeConfiguration.class,
        nodeDescription = "JSONPath expression from message",
        nodeDetails = "",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        icon = "functions",
        configDirective = "tbTransformationNodeJsonPathConfig"
)
public class TbJsonPathNode implements TbNode {

    TbJsonPathNodeConfiguration config;
    Configuration configurationJsonPath;
    JsonPath jsonPath;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsonPathNodeConfiguration.class);
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
            JsonNode jsonPathData = jsonPath.read(msg.getData(), this.configurationJsonPath);
            ctx.tellSuccess(createNewMsg(msg, jsonPathData));
        } catch (PathNotFoundException e) {
            ctx.tellFailure(msg, e);
        }
    }

    private TbMsg createNewMsg(TbMsg msg, JsonNode msgNode) {
        return TbMsg.newMsg(msg.getQueueName(), msg.getType(), msg.getOriginator(), msg.getMetaData(), JacksonUtil.toString(msgNode));
    }

    @Override
    public void destroy() {

    }
}
