/*
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

package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
    private String prefix;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException{
        this.config = TbNodeUtils.convert(configuration, TbGetAggregationNodeConfiguration.class);
        prefix = config.getPrefixTsKeyNames();

    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        String data = msg.getData();
        JsonNode json = null;
        ObjectNode outNode = null;
        String field = null;
        double sumFields = 0;
        int count = 0;
        Double avg = null;
        Double min = Double.MAX_VALUE;
        Double max = Double.MIN_VALUE;
        try {
            json = mapper.readTree(data);
            if (json.isObject()) {
                Iterator<String> iterator = json.fieldNames();
                while (iterator.hasNext()) {
                    field = iterator.next();
                    if (field.contains(prefix)) {
                        count += 1;
                        Double value = json.get(field).asDouble();
                        sumFields += value;
                        max = Math.max(max, value);
                        min = Math.min(min, value);
                        if (count != 0) {
                            avg = sumFields / count;
                        } else {
                            avg = null;
                        }
                    }
                }
            }
        } catch (IOException e) {
            ctx.tellFailure(msg, e);
        }
        try {
            outNode = mapper.createObjectNode();
            outNode.put("min", min);
            outNode.put("max", max);
            outNode.put("avg", avg);
            data = mapper.writeValueAsString(outNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        TbMsg newMsg = ctx.newMsg(msg.getType(), msg.getOriginator(), msg.getMetaData(), data);
        ctx.tellNext(newMsg, SUCCESS);

    }


    @Override
    public void destroy() {

    }
}
