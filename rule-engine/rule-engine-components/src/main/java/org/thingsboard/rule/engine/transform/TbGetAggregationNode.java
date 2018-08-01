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
        name = "get aggregation",
        configClazz = TbGetAggregationNodeConfiguration.class,
        nodeDescription = "",
        nodeDetails = "",
        uiResources = {"static/rulenode/rulenode-core-config.js"},//{"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "") //"tbTransformationNodeChangeOriginatorConfig")
public class TbGetAggregationNode implements TbNode {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbGetAggregationNodeConfiguration config;
    private List<String> prefixes;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetAggregationNodeConfiguration.class);
        prefixes = config.getPrefixesTsKeyNames();

    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        String data = msg.getData();
        List<String> containsPrefixes = new ArrayList<>();
        JsonNode json;
        ObjectNode outNode = mapper.createObjectNode();
        String field;
        try {
            json = mapper.readTree(data);
            if (json.isObject()) {
                for (String s : prefixes) {
                    Double avg = null;
                    Double min = Double.MAX_VALUE;
                    Double max = Double.MIN_VALUE;
                    double sumFields = 0;
                    int count = 0;
                    Iterator<String> iterator = json.fieldNames();
                    while (iterator.hasNext()) {
                        field = iterator.next();
                        if (field.contains(s)) {
                            count += 1;
                            Double value = json.get(field).asDouble();
                            sumFields += value;
                            max = Math.max(max, value);
                            min = Math.min(min, value);
                            if (count != 0) {
                                avg = Math.rint(100.0 * (sumFields / count)) / 100.0;
                            } else {
                                avg = null;
                            }
                        }
                    }
                    if(count == 0){
                        containsPrefixes.add(s);
                    }else {
                        outNode.put(s + "-min", min);
                        outNode.put(s + "-max", max);
                        outNode.put(s + "-avg", avg);
                    }
                }
            }
        } catch (IOException e) {
            ctx.tellFailure(msg, e);
        }
        try {
            if (containsPrefixes.isEmpty()) {
                data = mapper.writeValueAsString(outNode);
                TbMsg newMsg = ctx.newMsg(msg.getType(), msg.getOriginator(), msg.getMetaData(), data);
                ctx.tellNext(newMsg, SUCCESS);
            } else {
                data = mapper.writeValueAsString(outNode.nullNode());
                TbMsg newMsg = ctx.newMsg(msg.getType(), msg.getOriginator(), msg.getMetaData(), data);
                ctx.tellFailure(newMsg, new Exception("Message doesn't contains the prefixes: " + containsPrefixes.toString()));
            }

        } catch (JsonProcessingException e) {
            ctx.tellFailure(msg, e);
        }
    }

    @Override
    public void destroy() {

    }
}
