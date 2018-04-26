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
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


@Slf4j
public class NashornJsEngine implements org.thingsboard.rule.engine.api.ScriptEngine {

    public static final String MSG = "msg";
    public static final String METADATA = "metadata";
    public static final String MSG_TYPE = "msgType";

    private static final String JS_WRAPPER_PREFIX_TEMPLATE = "function %s(msgStr, metadataStr, msgType) { " +
            "    var msg = JSON.parse(msgStr); " +
            "    var metadata = JSON.parse(metadataStr); " +
            "    return JSON.stringify(%s(msg, metadata, msgType));" +
            "    function %s(%s, %s, %s) {";
    private static final String JS_WRAPPER_SUFFIX = "}" +
            "\n}";

    private static final ObjectMapper mapper = new ObjectMapper();
    private static NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
    private ScriptEngine engine = factory.getScriptEngine(new String[]{"--no-java"});

    private final String invokeFunctionName;

    public NashornJsEngine(String script, String functionName, String... argNames) {
        this.invokeFunctionName = "invokeInternal" + this.hashCode();
        String msgArg;
        String metadataArg;
        String msgTypeArg;
        if (argNames != null && argNames.length == 3) {
            msgArg = argNames[0];
            metadataArg = argNames[1];
            msgTypeArg = argNames[2];
        } else {
            msgArg = MSG;
            metadataArg = METADATA;
            msgTypeArg = MSG_TYPE;
        }
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, this.invokeFunctionName,
                functionName, functionName, msgArg, metadataArg, msgTypeArg);
        compileScript(jsWrapperPrefix + script + JS_WRAPPER_SUFFIX);
    }

    private void compileScript(String script) {
        try {
            engine.eval(script);
        } catch (ScriptException e) {
            log.warn("Failed to compile JS script: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Can't compile script: " + e.getMessage());
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
            if (msgData.has(MSG)) {
                JsonNode msgPayload = msgData.get(MSG);
                data = mapper.writeValueAsString(msgPayload);
            }
            if (msgData.has(METADATA)) {
                JsonNode msgMetadata = msgData.get(METADATA);
                metadata = mapper.convertValue(msgMetadata, new TypeReference<Map<String, String>>() {
                });
            }
            if (msgData.has(MSG_TYPE)) {
                messageType = msgData.get(MSG_TYPE).asText();
            }
            String newData = data != null ? data : msg.getData();
            TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData();
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
            String eval = ((Invocable)engine).invokeFunction(this.invokeFunctionName, inArgs[0], inArgs[1], inArgs[2]).toString();
            return mapper.readTree(eval);
        } catch (ScriptException | IllegalArgumentException th) {
            throw th;
        } catch (Throwable th) {
            th.printStackTrace();
            throw new RuntimeException("Failed to execute js script", th);
        }
    }

    public void destroy() {
        engine = null;
    }
}
