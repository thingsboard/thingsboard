/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.RuleNodeScriptFactory;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RuleNodeJsScriptEngine extends RuleNodeScriptEngine<JsInvokeService, JsonNode> {

    public RuleNodeJsScriptEngine(TenantId tenantId, JsInvokeService scriptInvokeService, String script, String... argNames) {
        super(tenantId, scriptInvokeService, script, argNames);
    }

    @Override
    protected Object[] prepareArgs(TbMsg msg) {
        String[] args = new String[3];
        if (msg.getData() != null) {
            args[0] = msg.getData();
        } else {
            args[0] = "";
        }
        args[1] = JacksonUtil.toString(msg.getMetaData().getData());
        args[2] = msg.getType();
        return args;
    }

    @Override
    protected List<TbMsg> executeUpdateTransform(TbMsg msg, JsonNode json) {
        if (json.isObject()) {
            return Collections.singletonList(unbindMsg(json, msg));
        } else if (json.isArray()) {
            List<TbMsg> res = new ArrayList<>(json.size());
            json.forEach(jsonObject -> res.add(unbindMsg(jsonObject, msg)));
            return res;
        }
        throw wrongResultType(json);
    }

    @Override
    protected TbMsg executeGenerateTransform(TbMsg prevMsg, JsonNode result) {
        if (!result.isObject()) {
            throw wrongResultType(result);
        }
        return unbindMsg(result, prevMsg);
    }

    @Override
    protected boolean executeFilterTransform(JsonNode json) {
        if (json.isBoolean()) {
            return json.asBoolean();
        }
        throw wrongResultType(json);
    }

    @Override
    protected Set<String> executeSwitchTransform(JsonNode result) {
        if (result.isTextual()) {
            return Collections.singleton(result.asText());
        }
        if (result.isArray()) {
            Set<String> nextStates = new HashSet<>();
            for (JsonNode val : result) {
                if (!val.isTextual()) {
                    throw wrongResultType(val);
                } else {
                    nextStates.add(val.asText());
                }
            }
            return nextStates;
        }
        throw wrongResultType(result);
    }

    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg) {
        return executeScriptAsync(msg);
    }

    @Override
    protected String executeToStringTransform(JsonNode result) {
        if (result.isTextual()) {
            return result.asText();
        }
        throw wrongResultType(result);
    }

    @Override
    protected JsonNode convertResult(Object result) {
        return JacksonUtil.toJsonNode(result != null ? result.toString() : null);
    }

    private static TbMsg unbindMsg(JsonNode msgData, TbMsg msg) {
        String data = null;
        Map<String, String> metadata = null;
        String messageType = null;
        if (msgData.has(RuleNodeScriptFactory.MSG)) {
            JsonNode msgPayload = msgData.get(RuleNodeScriptFactory.MSG);
            data = JacksonUtil.toString(msgPayload);
        }
        if (msgData.has(RuleNodeScriptFactory.METADATA)) {
            JsonNode msgMetadata = msgData.get(RuleNodeScriptFactory.METADATA);
            metadata = JacksonUtil.convertValue(msgMetadata, new TypeReference<>() {});
        }
        if (msgData.has(RuleNodeScriptFactory.MSG_TYPE)) {
            messageType = msgData.get(RuleNodeScriptFactory.MSG_TYPE).asText();
        }
        String newData = data != null ? data : msg.getData();
        TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData().copy();
        String newMessageType = StringUtils.isNotEmpty(messageType) ? messageType : msg.getType();
        return msg.transform()
                .type(newMessageType)
                .metaData(newMetadata)
                .data(newData)
                .build();
    }

    private TbScriptException wrongResultType(JsonNode result) {
        return new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, null, new ClassCastException("Wrong result type: " + result.getNodeType()));
    }

}
