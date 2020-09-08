/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
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
    private final JsInvokeService sandboxService;

    private final UUID scriptId;
    private final EntityId entityId;

    public RuleNodeJsScriptEngine(JsInvokeService sandboxService, EntityId entityId, String script, String... argNames) {
        this.sandboxService = sandboxService;
        this.entityId = entityId;
        try {
            this.scriptId = this.sandboxService.eval(JsScriptType.RULE_NODE_SCRIPT, script, argNames).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new IllegalArgumentException("Can't compile script: " + t.getMessage(), t);
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
            return TbMsg.transformMsg(msg, newMessageType, msg.getOriginator(), newMetadata, newData);
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
    public ListenableFuture<TbMsg> executeUpdateAsync(TbMsg msg) {
        ListenableFuture<JsonNode> result = executeScriptAsync(msg);
        return Futures.transformAsync(result, json -> {
            if (!json.isObject()) {
                log.warn("Wrong result type: {}", json.getNodeType());
                return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + json.getNodeType()));
            } else {
                return Futures.immediateFuture(unbindMsg(json, msg));
            }
        }, MoreExecutors.directExecutor());
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
    public ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg) throws ScriptException {
        return executeScriptAsync(msg);
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
    public ListenableFuture<Boolean> executeFilterAsync(TbMsg msg) {
        ListenableFuture<JsonNode> result = executeScriptAsync(msg);
        return Futures.transformAsync(result, json -> {
            if (!json.isBoolean()) {
                log.warn("Wrong result type: {}", json.getNodeType());
                return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + json.getNodeType()));
            } else {
                return Futures.immediateFuture(json.asBoolean());
            }
        }, MoreExecutors.directExecutor());
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
            String eval = sandboxService.invokeFunction(this.scriptId, inArgs[0], inArgs[1], inArgs[2]).get().toString();
            return mapper.readTree(eval);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ScriptException) {
                throw (ScriptException) e.getCause();
            } else if (e.getCause() instanceof RuntimeException) {
                throw new ScriptException(e.getCause().getMessage());
            } else {
                throw new ScriptException(e);
            }
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    private ListenableFuture<JsonNode> executeScriptAsync(TbMsg msg) {
        String[] inArgs = prepareArgs(msg);
        return Futures.transformAsync(sandboxService.invokeFunction(this.scriptId, inArgs[0], inArgs[1], inArgs[2]),
                o -> {
                    try {
                        return Futures.immediateFuture(mapper.readTree(o.toString()));
                    } catch (Exception e) {
                        if (e.getCause() instanceof ScriptException) {
                            return Futures.immediateFailedFuture(e.getCause());
                        } else if (e.getCause() instanceof RuntimeException) {
                            return Futures.immediateFailedFuture(new ScriptException(e.getCause().getMessage()));
                        } else {
                            return Futures.immediateFailedFuture(new ScriptException(e));
                        }
                    }
                }, MoreExecutors.directExecutor());
    }

    public void destroy() {
        sandboxService.release(this.scriptId);
    }
}
