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
package org.thingsboard.rule.engine.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rpc call request",
        configClazz = TbSendRpcRequestNodeConfiguration.class,
        nodeDescription = "Sends one-way RPC call to device",
        nodeDetails = "Expects messages with \"method\" and \"params\". Will forward response from device to next nodes.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeRpcRequestConfig"
)
public class TbSendRPCRequestNode implements TbNode {

    private Random random = new Random();
    private Gson gson = new Gson();
    private JsonParser jsonParser = new JsonParser();
    private TbSendRpcRequestNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSendRpcRequestNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        JsonObject json = jsonParser.parse(msg.getData()).getAsJsonObject();

        if (msg.getOriginator().getEntityType() != EntityType.DEVICE) {
            ctx.tellError(msg, new RuntimeException("Message originator is not a device entity!"));
        } else if (!json.has("method")) {
            ctx.tellError(msg, new RuntimeException("Method is not present in the message!"));
        } else if (!json.has("params")) {
            ctx.tellError(msg, new RuntimeException("Params are not present in the message!"));
        } else {
            int requestId = json.has("requestId") ? json.get("requestId").getAsInt() : random.nextInt();
            RuleEngineDeviceRpcRequest request = RuleEngineDeviceRpcRequest.builder()
                    .method(gson.toJson(json.get("method")))
                    .body(gson.toJson(json.get("params")))
                    .deviceId(new DeviceId(msg.getOriginator().getId()))
                    .requestId(requestId)
                    .timeout(TimeUnit.SECONDS.toMillis(config.getTimeoutInSeconds()))
                    .build();

            ctx.getRpcService().sendRpcRequest(request, ruleEngineDeviceRpcResponse -> {
                if (!ruleEngineDeviceRpcResponse.getError().isPresent()) {
                    TbMsg next = ctx.newMsg(msg.getType(), msg.getOriginator(), msg.getMetaData(), ruleEngineDeviceRpcResponse.getResponse().get());
                    ctx.tellNext(next, TbRelationTypes.SUCCESS);
                } else {
                    TbMsg next = ctx.newMsg(msg.getType(), msg.getOriginator(), msg.getMetaData(), wrap("error", ruleEngineDeviceRpcResponse.getError().get().name()));
                    ctx.tellNext(next, TbRelationTypes.FAILURE);
                    ctx.tellError(msg, new RuntimeException(ruleEngineDeviceRpcResponse.getError().get().name()));
                }
            });
        }
    }

    @Override
    public void destroy() {
    }

    private String wrap(String name, String body) {
        JsonObject json = new JsonObject();
        json.addProperty(name, body);
        return gson.toJson(json);
    }

}
