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

package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "get sum",
        configClazz = TbGetSumNodeConfiguration.class,
        nodeDescription = "",
        nodeDetails = "",
        uiResources = {"static/rulenode/rulenode-core-config.js"},//{"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "") //"tbTransformationNodeChangeOriginatorConfig")
public class TbGetSumNode implements TbNode {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbGetSumNodeConfiguration config;
    private String prefix;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetSumNodeConfiguration.class);
        prefix = config.getPrefixTsKeyNames();

    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        TbMsg newMsg;
        String data = msg.getData();
        JsonNode jsonNode;
        ObjectNode outNode = mapper.createObjectNode();
        double sum = 0;
        String field;
        try {
            jsonNode = mapper.readTree(data);
            Iterator<String> iterator = jsonNode.fieldNames();
            while (iterator.hasNext()) {
                field = iterator.next();
                if (field.contains(prefix)) {
                    double value = jsonNode.get(field).asDouble();
                    sum += value;
                }

            }
            try {
                if (sum == 0) {
                    data = mapper.writeValueAsString(outNode.nullNode());
                    newMsg = ctx.newMsg(msg.getType(), msg.getOriginator(), msg.getMetaData(), data);
                    ctx.tellNext(newMsg, FAILURE, new Exception("Message doesn't contains the prefix: " + prefix));
                } else {
                    outNode.put("Sum of " + prefix, sum);
                    data = mapper.writeValueAsString(outNode);
                    newMsg = ctx.newMsg(msg.getType(), msg.getOriginator(), msg.getMetaData(), data);
                    ctx.tellNext(newMsg, SUCCESS);
                }
            }catch (JsonProcessingException e){
                ctx.tellFailure(msg , e);
            }
        } catch (IOException e) {
            ctx.tellFailure(msg, e);
        }
    }

    @Override
    public void destroy() {}
}
