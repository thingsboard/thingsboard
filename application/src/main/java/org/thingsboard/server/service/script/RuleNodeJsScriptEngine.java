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
package org.thingsboard.server.service.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@Slf4j
public class RuleNodeJsScriptEngine implements org.thingsboard.rule.engine.api.ScriptEngine {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final JsSandboxService sandboxService;

    private final UUID scriptId;
    private final EntityId entityId;

    public RuleNodeJsScriptEngine(JsSandboxService sandboxService, EntityId entityId, String script, String... argNames) {
        this.sandboxService = sandboxService;
        this.entityId = entityId;
        try {
            this.scriptId = this.sandboxService.eval(JsScriptType.RULE_NODE_SCRIPT, script, argNames).get();
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't compile script: " + e.getMessage(), e);
        }
    }

    private static String[] prepareArgs(TbMsg msg) {
        try {
            String[] args = new String[3];
            if (msg.getData() != null) {
                args[0] = msg.getData();
            } else {
                args[0] = "";
            }
            args[1] = mapper.writeValueAsString(msg.getMetaData().getData());
            args[2] = msg.getType();
            return args;
        } catch (Throwable th) {
            throw new IllegalArgumentException("Cannot bind js args", th);
        }
    }

    private static TbMsg unbindMsg(JsonNode msgData, TbMsg msg) {
        try {
            String data = null;
            Map<String, String> metadata = null;
            String messageType = null;
            if (msgData.has(RuleNodeScriptFactory.MSG)) {
                JsonNode msgPayload = msgData.get(RuleNodeScriptFactory.MSG);
                data = mapper.writeValueAsString(msgPayload);
            }
            if (msgData.has(RuleNodeScriptFactory.METADATA)) {
                JsonNode msgMetadata = msgData.get(RuleNodeScriptFactory.METADATA);
                metadata = mapper.convertValue(msgMetadata, new TypeReference<Map<String, String>>() {
                });
            }
            if (msgData.has(RuleNodeScriptFactory.MSG_TYPE)) {
                messageType = msgData.get(RuleNodeScriptFactory.MSG_TYPE).asText();
            }
            String newData = data != null ? data : msg.getData();
            TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData().copy();
            String newMessageType = !StringUtils.isEmpty(messageType) ? messageType : msg.getType();
            return new TbMsg(msg.getId(), newMessageType, msg.getOriginator(), newMetadata, newData, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition());
        } catch (Throwable th) {
            th.printStackTrace();
            throw new RuntimeException("Failed to unbind message data from javascript result", th);
        }
    }

    @Override
    public TbMsg executeUpdate(TbMsg msg) throws ScriptException {
        JsonNode result = executeScript(msg);
        if (!result.isObject()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
        return unbindMsg(result, msg);
    }

    @Override
    public TbMsg executeGenerate(TbMsg prevMsg) throws ScriptException {
        JsonNode result = executeScript(prevMsg);
        if (!result.isObject()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
        return unbindMsg(result, prevMsg);
    }

    @Override
    public JsonNode executeJson(TbMsg msg) throws ScriptException {
        return executeScript(msg);
    }

    @Override
    public String executeToString(TbMsg msg) throws ScriptException {
        JsonNode result = executeScript(msg);
        if (!result.isTextual()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
        return result.asText();
    }

    @Override
    public boolean executeFilter(TbMsg msg) throws ScriptException {
        JsonNode result = executeScript(msg);
        if (!result.isBoolean()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
        return result.asBoolean();
    }

    @Override
    public Set<String> executeSwitch(TbMsg msg) throws ScriptException {
        JsonNode result = executeScript(msg);
        if (result.isTextual()) {
            return Collections.singleton(result.asText());
        } else if (result.isArray()) {
            Set<String> nextStates = Sets.newHashSet();
            for (JsonNode val : result) {
                if (!val.isTextual()) {
                    log.warn("Wrong result type: {}", val.getNodeType());
                    throw new ScriptException("Wrong result type: " + val.getNodeType());
                } else {
                    nextStates.add(val.asText());
                }
            }
            return nextStates;
        } else {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
    }

    private JsonNode executeScript(TbMsg msg) throws ScriptException {
        try {
            String[] inArgs = prepareArgs(msg);
            String eval = sandboxService.invokeFunction(this.scriptId, this.entityId, inArgs[0], inArgs[1], inArgs[2]).get().toString();
            return mapper.readTree(eval);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ScriptException) {
                throw (ScriptException)e.getCause();
            } else {
                throw new ScriptException(e);
            }
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    public void destroy() {
        sandboxService.release(this.scriptId, this.entityId);
    }
}
